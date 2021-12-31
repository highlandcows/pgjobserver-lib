object Helpers {
  def sysPropOrDefault(propName: String, default: String): String =
    Option(System.getProperty(propName)).getOrElse(default)
}
