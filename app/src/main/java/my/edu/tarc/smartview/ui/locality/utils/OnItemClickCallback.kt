package my.edu.tarc.smartview.ui.locality.utils

import my.edu.tarc.smartview.ui.locality.model.ModelLocality

interface OnItemClickCallback {
    fun onItemMainClicked(modelLocality: ModelLocality?)
}