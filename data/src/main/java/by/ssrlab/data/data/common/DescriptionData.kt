package by.ssrlab.data.data.common

import by.ssrlab.data.data.remove.Image

interface DescriptionData {
    val pk: Int
    val logo: String
    val image: Image
}