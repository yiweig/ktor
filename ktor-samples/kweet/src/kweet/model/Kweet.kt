package kweet.model

import org.joda.time.*

data class Kweet(val id: Int, val userId: String, val text: String, val date: DateTime)