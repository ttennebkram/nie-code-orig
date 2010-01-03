<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<xsl:param name="object" />

<xsl:template match="/nie_config">
<html>
<head>
<title>SearchNames Admin / Show All Config</title>
</head>
<body>

<h2>SearchNames Admin / Show All Config</h2>

<xsl:apply-templates select="search_names" mode="short" />

<xsl:apply-templates select="search_names/search_engine_info" />

<h3>Mappings</h3>

<table border="1">
<tr>
<th width="45%">Term(s)</th>
<th>URL(s)</th>
</tr>
<xsl:apply-templates select="search_names/fixed_redirection_map/map" />
</table>

</body>
</html>
</xsl:template> <!-- End of root template -->


<xsl:template match="search_names" mode="short">
	<b>SearchNames running on port:</b>
	<xsl:value-of select="@port" />
	<br />

	<b>FULL Search Names Server URL:</b>
	<xsl:value-of select="search_names_url" />
	<br />
</xsl:template>

<xsl:template match="search_engine_info">
<b>Search Engine Info:</b> <br />

	<b>Vendor (opt):</b>
	<xsl:value-of select="vendor" />
	<br />

	<b>Search URL:</b>
	<xsl:value-of select="search_url" />
	<br />

	<b>No action indicator fields (if any):</b><br />
	<xsl:for-each select="no_action_indicator_field">
		&#160; <xsl:value-of select="." /><br />
	</xsl:for-each>

	<b>Suggestion marker text (multiple is OK):</b><br />
	<xsl:for-each select="suggestion_marker_text">
		&#160; <xsl:value-of select="." /><br />
	</xsl:for-each>

</xsl:template>



<!-- A single map entry -->
<!-- ================== -->

<xsl:template match="map">

<!--
<big><b>Map</b></big><br />
<b>Terms:</b>
-->

<tr>

<td>
	<xsl:for-each select="term">
		&quot;<xsl:value-of select="." />&quot;
		&#160;
	</xsl:for-each>
	<br />
</td>

<!--
<b>URLs and Alternate Terms:</b><br />
-->

<td>
	<xsl:for-each select="url">
		&#160; <b>URL:</b> &#160;
		<small><xsl:value-of select="text()" /></small><br />
	</xsl:for-each>
	<xsl:for-each select="alternate_term">
		&#160; <b>Alternate Term:</b> &#160;
		<xsl:value-of select="text()" /><br />
	</xsl:for-each>
</td>

</tr>

<!--
<p />
-->
</xsl:template>



</xsl:stylesheet>
