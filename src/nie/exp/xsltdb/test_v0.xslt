<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- The root -->
<xsl:template match="/">
<items>
	<!-- Indicies and constraints -->
	<xsl:apply-templates select="items" />
</items>
</xsl:template>

<xsl:template match="items">
	<xsl:apply-templates
		select="item"
		mode="master"
	/>
</xsl:template>

<xsl:template match="item" mode="master" >
	<!--
	<itema>
		<xsl:value-of select="position()" />
	</itema>
	-->
	<xsl:apply-templates select="."
		mode="slave"
	>
		<xsl:with-param name="nth" select="position()" />
	</xsl:apply-templates>
	<!-- The closing comma, if needed -->
	<xsl:if test="position() &lt; last()">
		NOT LAST
	</xsl:if>
</xsl:template>


<xsl:template match="item" mode="slave">
	<xsl:param name="nth" />
		TEST nth = <xsl:value-of select="$nth" />

</xsl:template>

</xsl:stylesheet>
