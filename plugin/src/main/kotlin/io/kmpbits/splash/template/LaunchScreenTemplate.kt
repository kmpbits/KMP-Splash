package io.kmpbits.splash.template

internal object LaunchScreenTemplate {

    /**
     * Generates a LaunchScreen.storyboard XML.
     *
     * [backgroundColor] must be a hex string like "#FF0000" or "#RRGGBB".
     * [logoResourceName] is the name of an asset in the Xcode asset catalogue
     *   (without extension). Pass null to omit the image view entirely.
     */
    fun generate(
        backgroundColor: String,
        logoResourceName: String? = null,
    ): String {
        val (r, g, b) = parseHexColor(backgroundColor)
        val subviews = if (logoResourceName != null) imageViewXml(logoResourceName) else ""
        val constraints = if (logoResourceName != null) imageConstraintsXml() else ""

        return """<?xml version="1.0" encoding="UTF-8"?>
<document
    type="com.apple.InterfaceBuilder3.CocoaTouch.Storyboard.XIB"
    version="3.0"
    toolsVersion="21701"
    targetRuntime="AppleCocoaTouch"
    propertyAccessControl="none"
    useAutolayout="YES"
    launchScreen="YES"
    useTraitCollections="YES"
    useSafeAreas="YES"
    colorMatched="YES"
    initialViewController="01J-lp-oVM">
    <device id="retina6_12" orientation="portrait" appearance="light"/>
    <dependencies>
        <deployment identifier="iOS"/>
        <plugIn identifier="com.apple.InterfaceBuilder.IBCocoaTouchPlugin" version="21679"/>
        <capability name="Safe area layout guides" minToolsVersion="9.0"/>
        <capability name="documents saved in the Xcode 8 format" minToolsVersion="8.0"/>
    </dependencies>
    <scenes>
        <scene sceneID="tne-QT-ifu">
            <objects>
                <viewController
                    id="01J-lp-oVM"
                    sceneMemberID="viewController">
                    <view
                        key="view"
                        contentMode="scaleToFill"
                        id="Ze5-6b-2t3">
                        <rect key="frame" x="0.0" y="0.0" width="393" height="852"/>
                        <autoresizingMask key="autoresizingMask" widthSizable="YES" heightSizable="YES"/>
                        $subviews
                        <color
                            key="backgroundColor"
                            red="$r"
                            green="$g"
                            blue="$b"
                            alpha="1"
                            colorSpace="custom"
                            customColorSpace="sRGB"/>
                        <viewLayoutGuide type="safeArea" id="Bcu-3y-fUS"/>
                        $constraints
                    </view>
                </viewController>
                <placeholder
                    placeholderIdentifier="IBFirstResponder"
                    id="iYj-Kq-Eg1"
                    userLabel="First Responder"
                    sceneMemberID="firstResponder"/>
            </objects>
            <point key="canvasLocation" x="53" y="375"/>
        </scene>
    </scenes>
</document>"""
    }

    private fun imageViewXml(resourceName: String): String = """<subviews>
                            <imageView
                                clipsSubviews="YES"
                                userInteractionEnabled="NO"
                                contentMode="scaleAspectFit"
                                horizontalHuggingPriority="251"
                                verticalHuggingPriority="251"
                                image="$resourceName"
                                translatesAutoresizingMaskIntoConstraints="NO"
                                id="splash-logo-view">
                                <rect key="frame" x="96.5" y="376" width="200" height="100"/>
                            </imageView>
                        </subviews>"""

    private fun imageConstraintsXml(): String = """<constraints>
                            <constraint
                                firstItem="splash-logo-view"
                                firstAttribute="centerX"
                                secondItem="Ze5-6b-2t3"
                                secondAttribute="centerX"
                                id="cx-center"/>
                            <constraint
                                firstItem="splash-logo-view"
                                firstAttribute="centerY"
                                secondItem="Ze5-6b-2t3"
                                secondAttribute="centerY"
                                id="cy-center"/>
                            <constraint
                                firstItem="splash-logo-view"
                                firstAttribute="width"
                                constant="200"
                                id="w-logo"/>
                            <constraint
                                firstItem="splash-logo-view"
                                firstAttribute="height"
                                constant="100"
                                id="h-logo"/>
                        </constraints>"""

    /** Returns a triple of sRGB component strings in [0,1] range. */
    private fun parseHexColor(hex: String): Triple<String, String, String> {
        val clean = hex.trimStart('#').also {
            require(it.length == 6) { "backgroundColor must be #RRGGBB, got: $hex" }
        }
        fun channel(start: Int) =
            (clean.substring(start, start + 2).toInt(16) / 255.0)
                .toBigDecimal()
                .setScale(10, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()

        return Triple(channel(0), channel(2), channel(4))
    }
}
