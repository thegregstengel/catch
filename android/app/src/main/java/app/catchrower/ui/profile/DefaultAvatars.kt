package app.catchrower.ui.profile

import androidx.annotation.DrawableRes
import app.catchrower.R

/**
 * Built-in profile photos. Stored as `catch://avatar/<id>` in [ProfileEntity.photoUri].
 */
object DefaultAvatars {
    const val URI_SCHEME = "catch"
    const val URI_HOST = "avatar"

    data class Avatar(
        val id: String,
        @DrawableRes val resId: Int,
    )

    val all: List<Avatar> = listOf(
        Avatar("man_beard", R.drawable.avatar_man_beard),
        Avatar("man_athletic", R.drawable.avatar_man_athletic),
        Avatar("woman_short", R.drawable.avatar_woman_short),
        Avatar("woman_long", R.drawable.avatar_woman_long),
        Avatar("elder_man", R.drawable.avatar_elder_man),
        Avatar("elder_woman", R.drawable.avatar_elder_woman),
        Avatar("teen_boy", R.drawable.avatar_teen_boy),
        Avatar("teen_girl", R.drawable.avatar_teen_girl),
        Avatar("kid_boy", R.drawable.avatar_kid_boy),
        Avatar("kid_girl", R.drawable.avatar_kid_girl),
        Avatar("rower", R.drawable.avatar_rower),
        Avatar("astronaut", R.drawable.avatar_astronaut),
        Avatar("robot_blue", R.drawable.avatar_robot_blue),
        Avatar("robot_gold", R.drawable.avatar_robot_gold),
        Avatar("alien", R.drawable.avatar_alien),
        Avatar("cat", R.drawable.avatar_cat),
        Avatar("dog", R.drawable.avatar_dog),
        Avatar("fox", R.drawable.avatar_fox),
        Avatar("owl", R.drawable.avatar_owl),
        Avatar("otter", R.drawable.avatar_otter),
    )

    fun uriFor(id: String): String = "$URI_SCHEME://$URI_HOST/$id"

    fun uriFor(avatar: Avatar): String = uriFor(avatar.id)

    @DrawableRes
    fun drawableRes(photoUri: String?): Int? {
        if (photoUri.isNullOrBlank()) return null
        if (!photoUri.startsWith("$URI_SCHEME://$URI_HOST/")) return null
        val id = photoUri.removePrefix("$URI_SCHEME://$URI_HOST/")
        return all.firstOrNull { it.id == id }?.resId
    }

    fun isDefault(photoUri: String?): Boolean = drawableRes(photoUri) != null
}
