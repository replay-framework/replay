package model

@JvmInline
value class Identifier<T>(val value: Long) : Comparable<Identifier<T>> {

  override fun toString(): String =
    value.toString()

  override fun compareTo(other: Identifier<T>) =
    value.compareTo(other.value)

  fun getTypeString(): String =
    this::class.java.simpleName

}
