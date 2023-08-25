package my.edu.tarc.smartview.ui.locality.model

import java.io.Serializable

class ModelLocality : Serializable {
    var id: String? = null
    var nameLocality: String? = null
    var thumbLocality: String? = null
    var ratingText: String? = null
    var addressLocality: String? = null
    var aggregateRating = 0.0
}