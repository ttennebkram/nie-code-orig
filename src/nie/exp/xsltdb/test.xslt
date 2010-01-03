<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<items>
	<xsl:apply-templates select="items/item" mode="master" />
</items>
</xsl:template>

<xsl:template match="item" mode="master" >
	Test1 pos() = <xsl:value-of select="position()" />
	<xsl:apply-templates select="."
		mode="slave"
	>
		<xsl:with-param name="nth" select="position()" />
	</xsl:apply-templates>
	<xsl:if test="position() &lt; last()">
		NOT LAST
	</xsl:if>
</xsl:template>

<xsl:template match="item" mode="slave" >
	<xsl:param name="nth" />
	<!-- this always gives 0 with Java 140 or 141 -->
	Test 2 nth = <xsl:value-of select="$nth" />
	<!-- always 1
	Test 3 pos() = <xsl:value-of select="position()" />
	-->
</xsl:template>

</xsl:stylesheet>
