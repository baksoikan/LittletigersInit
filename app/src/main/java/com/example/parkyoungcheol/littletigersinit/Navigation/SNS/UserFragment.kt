package com.example.parkyoungcheol.littletigersinit.Navigation.SNS

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.parkyoungcheol.littletigersinit.Chat.ChatLoginActivity
import com.example.parkyoungcheol.littletigersinit.MainActivity
import com.example.parkyoungcheol.littletigersinit.Model.AlarmDTO
import com.example.parkyoungcheol.littletigersinit.Model.ContentDTO
import com.example.parkyoungcheol.littletigersinit.Model.FollowDTO
import com.example.parkyoungcheol.littletigersinit.R
import com.example.parkyoungcheol.littletigersinit.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import java.util.*

class UserFragment : Fragment() {

    val PICK_PROFILE_FROM_ALBUM = 10

    // Firebase
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

    //private String destinationUid;
    var uid: String? = null
    var currentUserUid: String? = null

    var fragmentView: View? = null

    var fcmPush: FcmPush? = null


    var followListenerRegistration: ListenerRegistration? = null
    var followingListenerRegistration: ListenerRegistration? = null
    var imageprofileListenerRegistration: ListenerRegistration? = null
    var recyclerListenerRegistration: ListenerRegistration? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)

        // Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fcmPush = FcmPush()

        currentUserUid = auth?.currentUser?.uid

        if (arguments != null) {

            uid = arguments!!.getString("destinationUid")

            // 본인 계정인 경우 -> 로그아웃, Toolbar 기본으로 설정 , 사진추가 아이콘 보이기
            if (uid != null && uid == currentUserUid) {

                fragmentView!!.account_btn_follow_signout.text = getString(R.string.signout)
                fragmentView!!.account_btn_follow_signout.setTextColor(Color.BLACK)
                fragmentView!!.account_btn_follow_signout.setBackgroundResource(R.drawable.rectangle_btn_default)
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    startActivity(Intent(activity, ChatLoginActivity::class.java))
                    activity?.finish()
                    auth?.signOut()
                }

                // Profile Image Click Listener
                fragmentView?.account_iv_profile?.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                        //앨범 오픈
                        var photoPickerIntent = Intent(Intent.ACTION_PICK)
                        photoPickerIntent.type = "image/*"
                        activity!!.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
                    }
                }

            } else {
                showHide(fragmentView!!.add_photo)
                fragmentView!!.account_btn_follow_signout.setBackgroundResource(R.drawable.rectangle_btn)
                fragmentView!!.account_btn_follow_signout.text = getString(R.string.follow)
                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.ARmessageBtn.visibility = View.GONE
                mainActivity.ChatBtn.visibility = View.GONE
                mainActivity.ARbtn.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE

                mainActivity.toolbar_username.text = arguments!!.getString("userId")

                mainActivity.toolbar_btn_back.setOnClickListener { mainActivity.bottom_navigation.selectedItemId = R.id.action_home }

                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    requestFollow()
                }
            }


        }

        getFollowing()
        getFollower()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()

        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        getProfileImage()
    }

    fun showHide(view: View) {
        view.visibility = if (view.visibility == View.VISIBLE) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    fun getProfileImage() {
        imageprofileListenerRegistration = firestore?.collection("profileImages")?.document(uid!!)
                ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

                    if (documentSnapshot?.data != null) {
                        val url = documentSnapshot?.data!!["image"]
                        activity?.let {
                            Glide.with(it)
                                    .load(url)
                                    .apply(RequestOptions().circleCrop()).into(fragmentView!!.account_iv_profile)
                        }
                    } else {
                        cover_account_iv_profile.setImageResource(R.drawable.ic_account)
                    }
                }

    }


    fun getFollowing() {
        followingListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView!!.account_tv_following_count.text = followDTO?.followingCount.toString()
        }
    }


    fun getFollower() {
        followListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount.toString()
            if (followDTO?.followers?.containsKey(currentUserUid)!!) {

                fragmentView!!.account_btn_follow_signout.setBackgroundResource(R.drawable.rectangle_btn_default)
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                fragmentView!!.account_btn_follow_signout.setTextColor(Color.BLACK)
                fragmentView?.account_btn_follow_signout
                        ?.background
                        ?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
            } else {

                if (uid != currentUserUid) {
                    fragmentView!!.account_btn_follow_signout.setBackgroundResource(R.drawable.rectangle_btn)
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    fragmentView!!.account_btn_follow_signout.setTextColor(Color.WHITE)
                    fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                }
            }

        }

    }


    fun requestFollow() {


        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction

            }
            // Unstar the post and remove self from stars
            if (followDTO?.followings?.containsKey(uid)!!) {

                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            } else {

                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true


                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {


                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {

                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true

            }// Star the post and add self to stars

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }

    }

    fun followerAlarm(destinationUid: String) {

        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser!!.email
        alarmDTO.uid = auth?.currentUser!!.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        var message = auth?.currentUser!!.email + getString(R.string.alarm_follow)
        fcmPush?.sendMessage(destinationUid, "알림 메세지 입니다.", message)
    }


    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val contentDTOs: ArrayList<ContentDTO>
        val contentUidList: ArrayList<String>

        init {

            contentDTOs = ArrayList()
            contentUidList = ArrayList()

            // 나의 사진만 찾기
            contentDTOs.clear()
            contentUidList.clear()
            recyclerListenerRegistration = firestore
                    ?.collection("images")
                    ?.orderBy("timestamp", Query.Direction.DESCENDING)
                    ?.whereEqualTo("uid", uid)
                    ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        if (querySnapshot == null) return@addSnapshotListener
                        for (snapshot in querySnapshot!!.documents) {
                            contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                            contentUidList.add(snapshot.id)
                        }
                        account_tv_post_count.text = contentDTOs.size.toString()
                        notifyDataSetChanged()
                    }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val width = resources.displayMetrics.widthPixels / 3

            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                    .load(contentDTOs[position].imageUrl)
                    .apply(RequestOptions().centerCrop())
                    .into(imageview)

            imageview.setOnClickListener {
                val fragment = UserDetailViewFragment()
                val bundle = Bundle()

                // UserDetailViewFragment로 해당 정보(해당 게시글 이미지 url, 해당 게시글을 게시한 유저의 uid) 전송
                bundle.putString("imageUrl", contentDTOs[position].imageUrl)
                bundle.putString("uid", contentDTOs[position].uid)
                bundle.putString("contentUid", contentUidList[position])

                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, fragment)
                        .commit()
            }
        }

        override fun getItemCount(): Int {

            return contentDTOs.size
        }

        // RecyclerView CustomListAdapter - View Holder
        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }

    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
        followingListenerRegistration?.remove()
        imageprofileListenerRegistration?.remove()
        recyclerListenerRegistration?.remove()
    }
}