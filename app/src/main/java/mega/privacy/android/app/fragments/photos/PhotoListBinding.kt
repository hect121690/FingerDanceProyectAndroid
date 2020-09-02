package mega.privacy.android.app.fragments.photos

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.DraweeView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.android.synthetic.main.item_photo_browse.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util
import java.io.File

@Suppress("UNCHECKED_CAST")
@BindingAdapter("items")
fun setItems(listView: RecyclerView, items: List<PhotoNode>?) {
    items?.let {
        (listView.adapter as ListAdapter<PhotoNode, PhotoViewHolder>).submitList(it)
    }
}

@BindingAdapter("thumbnail", "selected")
fun setThumbnail(imageView: SimpleDraweeView, file: File?, selected: Boolean) {
    with(imageView) {
        if (file == null) setImageResource(R.drawable.ic_image_thumbnail) else setImageURI(
            Uri.fromFile(
                file
            )
        )

        if (selected) {
            hierarchy.roundingParams = getRoundingParams(context)
        } else {
            hierarchy.roundingParams = null
        }
    }
}

@BindingAdapter("thumbnail", "item_selected")
fun setSearchThumbnail(imageView: SimpleDraweeView, file: File?, selected: Boolean) {
    with(imageView) {
        if (selected) {
            setActualImageResource(R.drawable.ic_select_folder)
        } else {
            if (file == null) setImageResource(R.drawable.ic_image_thumbnail) else setImageURI(
                Uri.fromFile(
                    file
                )
            )
        }
    }
}

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

private var roundingParams: RoundingParams? = null

fun getRoundingParams(context: Context): RoundingParams? {
    roundingParams?.let {
        return it
    }

    roundingParams = RoundingParams.fromCornersRadius(
        Util.dp2px(
            context.resources.getDimension(R.dimen.photo_selected_icon_round_corner_radius),
            context.resources.displayMetrics
        ).toFloat()
    )

    roundingParams?.apply {
        setBorder(
            context.resources.getColor(R.color.accentColor), Util.dp2px(
                context.resources.getDimension(R.dimen.photo_selected_border_width),
                context.resources.displayMetrics
            ).toFloat()
        )
    }

    return roundingParams
}

