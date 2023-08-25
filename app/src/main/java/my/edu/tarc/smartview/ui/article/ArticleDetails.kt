package my.edu.tarc.smartview.ui.article

import android.os.Parcel
import android.os.Parcelable

data class ArticleDetails(
    val category: String? = null,
    val uid: String? = null,
    val image: String? = null,
    val title: String? = null,
    val detail: String? = null,
    val time: String? = null,
    val name: String? = null,
    val city: String? = null,
    val state: String? = null,
    val articleID: String? = null,
    val bookmarkTime: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(category)
        parcel.writeString(uid)
        parcel.writeString(image)
        parcel.writeString(title)
        parcel.writeString(detail)
        parcel.writeString(time)
        parcel.writeString(name)
        parcel.writeString(city)
        parcel.writeString(state)
        parcel.writeString(articleID)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ArticleDetails> {
        override fun createFromParcel(parcel: Parcel): ArticleDetails {
            return ArticleDetails(parcel)
        }

        override fun newArray(size: Int): Array<ArticleDetails?> {
            return arrayOfNulls(size)
        }
    }
}

