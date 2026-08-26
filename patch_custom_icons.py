with open("app/src/main/java/com/example/ui/theme/CustomIcons.kt", "a") as f:
    f.write("""
    val Pencil: ImageVector
        get() = ImageVector.Builder(
            name = "Pencil",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 17.25f)
                lineTo(3f, 21f)
                lineTo(6.75f, 21f)
                lineTo(17.81f, 9.94f)
                lineTo(14.06f, 6.19f)
                lineTo(3f, 17.25f)
                close()
                moveTo(20.71f, 7.04f)
                curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
                lineTo(18.37f, 3.29f)
                curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
                lineTo(15.13f, 5.12f)
                lineTo(18.88f, 8.87f)
                lineTo(20.71f, 7.04f)
                close()
            }
        }.build()
}
""")
