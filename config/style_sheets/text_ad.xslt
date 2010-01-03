<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">


<!--	Root Template
	===========================
	Start at the top items tag
-->
<xsl:template match="/items">

<table border="0" cellspacing="0" cellpadding="5">
<tr>

<td valign="top">

<center>
<font size="1">Advertiser Links</font>
</center>

<!--
<style>
.text_ad{cursor:pointer;cursor:hand}
</style>
-->
<!-- don't need vertical spacer any more
<img width="0" height="160" />
<br />
-->

<!-- For each individual child item -->
<xsl:apply-templates select="item" />
</td>

</tr>
</table>
</xsl:template> <!-- End of root template -->


<!--	Individual Text Ad
	===========================
-->

<xsl:template match="item">


<!-- Set the width -->
<!-- <xsl:variable name="width" select='"220"' /> -->
<xsl:variable name="width" select='"160"' />
<!-- <xsl:variable name="width" select='"130"' /> -->
<!-- <xsl:variable name="width" select='"150"' /> -->


<!-- border was bgcolor="#000080", then a0a0d8 -->
<table bgcolor="#374b54" cellpadding="1" cellspacing="0">
<xsl:attribute name="width">
	<xsl:value-of select="$width" />
</xsl:attribute>

<!--
<xsl:attribute name="onClick">
-->
	<!--
	window.open('<xsl:value-of select="normalize-space(url/text())" />')
	-->
<!--
	window.open('<xsl:value-of select="normalize-space(sn_href/text())" />')
</xsl:attribute>
-->

<!-- onClick="window.open('wateva.html')" -->
<!-- location.href='<xsl:value-of select="normalize-space(url/text())" />' -->


<tr>
<td>

<table bgcolor="#CCFFFF" cellpadding="7" cellspacing="0" width="100%" class="text_ad">
<!-- <table bgcolor="#e3effb" cellpadding="7" cellspacing="0" width="100%" class="text_ad"> -->

<tr>
<td>
<xsl:attribute name="id">myad<xsl:value-of select="position()" /></xsl:attribute>
<!--
<xsl:attribute name="onMouseOver">if(navigator.appName!='Netscape'){document.all['myad<xsl:value-of select="position()" />'].style.backgroundColor='#88FFFF';}</xsl:attribute>
<xsl:attribute name="onMouseOut">if(navigator.appName!='Netscape'){document.all['myad<xsl:value-of select="position()" />'].style.backgroundColor='#CCFFFF';}</xsl:attribute>
-->


<!-- dark blue was "#000080" , then 374b54 -->
<font face="Arial-Black,Arial,'MS Sans Serif',Geneva,sans-serif" size="-1" color="#000080">

<!-- The Title -->
<dt><nobr><b>
	<xsl:value-of select="url/title" />
</b></nobr></dt>

</font>

<font size="1" face="Arial">

<!-- The Description -->
<xsl:for-each select="url/description_line">
	<dt><nobr>

	<xsl:choose>
		<xsl:when test="position() &lt; last()">
			<xsl:value-of select="text()" />
			<!--
			<xsl:if test="not text()">
			<xsl:if test="not(normalize-space(.))" >
			-->
			<xsl:if test="not(normalize-space(text()))" >
				&#160;
			</xsl:if>
		</xsl:when>
		<xsl:otherwise>
			<a target="_blank">
				<xsl:attribute name="href">
					<xsl:value-of select="../../sn_href/text()" />
				</xsl:attribute>
				<xsl:value-of select="text()" />
				<xsl:if test="not(normalize-space(text()))" >
					click here
				</xsl:if>
			</a>
		</xsl:otherwise>
	</xsl:choose>
	</nobr></dt>
</xsl:for-each>

<!-- The URL -->
<!--
<dt><a target="_blank">
<xsl:attribute name="href">
-->
	<!--
	<xsl:value-of select="normalize-space(text(url))" />
	<xsl:for-each select="url">
		<xsl:value-of select="text()" />
	</xsl:for-each>
	-->
<!--
	<xsl:for-each select="sn_href">
		<xsl:value-of select="text()" />
	</xsl:for-each>
</xsl:attribute>
<b>
-->
	<!--
	<xsl:value-of select="url" />
	-->
	<!--
	<xsl:for-each select="url">
		<xsl:value-of select="substring(normalize-space(text()),8)" />
	</xsl:for-each>
	-->
<!--
</b>
</a></dt>
-->

</font>

</td></tr>
</table>
</td></tr>
</table>

<!-- Add a spacer -->
<img width="0" height="7" />

</xsl:template>




</xsl:stylesheet>
