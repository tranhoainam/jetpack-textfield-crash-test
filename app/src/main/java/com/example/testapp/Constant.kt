package com.example.testapp

object Regex {
    const val ALLOWED_NAME_CHAR = "[0-9a-zA-ZÀÁÂÃÈÉÊẾÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽếềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵỷỹ\\\\s]"
    const val MENTION = "@\\[\\{user-\\d*\\,$ALLOWED_NAME_CHAR+\\}\\]"
    const val URL = "(?:(?:https?|http):\\/\\/|\\b(?:[a-z\\d]+\\.[^\\s\\\"]))(?:(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))?\\))+(?:\\((?:[^\\s()<>]+|(?:\\(?:[^\\s()<>]+\\)))?\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))?"
    const val URL_OR_MENTION = "$MENTION|$URL"
    val MENTIONING_REGEX = Regex(URL_OR_MENTION)
}

object Constant {
    const val CLICKABLE_TYPE_MENTION = "CLICKABLE_TYPE_MENTION"
    const val CLICKABLE_TYPE_URL = "CLICKABLE_TYPE_URL"
    const val START_INDEX_TO_GET_USER_ID = 8 // Index of text after "-" in mention format
    const val OPEN_WEBVIEW = "OPEN_WEBVIEW"
    const val OPEN_PROFILE = "OPEN_PROFILE"
}