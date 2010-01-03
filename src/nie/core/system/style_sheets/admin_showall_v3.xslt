<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<xsl:param name="object" />

<xsl:template match="/nie_config">
<html>
<head>
<title>Search Tuning Admin / Show All Config</title>
</head>
<body>

<h2>Search Tuning Admin / Show All Config</h2>

<h3>Startup Info:</h3>

<b>Loaded at:</b>
<xsl:value-of select="@_start_time" /><br />

<b>Config URI:</b><br />
(as entered) = <xsl:value-of select="@_config_uri" /><br />
(cannonical) = <xsl:value-of select="@_full_config_uri" /><br />

<b>Version:</b>
<xsl:value-of select="@_version" /><br />

<!--
<b>Product and Options:</b>
<pre><br />
<xsl:value-of select="@_version_and_config" /><br />
</pre>
-->


<!-- xsl:apply-templates select="search_names" mode="short" / -->
<xsl:apply-templates select="search_tuning" mode="short" />

<!-- xsl:apply-templates select="search_names/search_engine_info" / -->
<xsl:apply-templates select="search_tuning/search_engine_info" />

<h3>Mappings</h3>

<table border="1">
<tr>
<th width="45%">Term(s)</th>
<th>URL(s)</th>
</tr>
<!-- xsl:apply-templates select="search_names/fixed_redirection_map/map" / -->
<xsl:apply-templates select="search_tuning/fixed_redirection_map/map" />
</table>

</body>
</html>
</xsl:template> <!-- End of root template -->

<!-- xsl:template match="search_names" mode="short" -->
<xsl:template match="search_tuning" mode="short">
	<!--
	<b>Search Tuning running on port:</b>
	<xsl:value-of select="@port" />
	<br />
	-->

	<!-- b>FULL Search Names Server URL:</b -->
	<b>FULL NIE Server URL (default port # is 80):</b>
	<!-- xsl:value-of select="search_names_url" / -->
	<xsl:value-of select="nie_server_url" />
	<br />
</xsl:template>

<xsl:template match="search_engine_info">
<h3>Search Engine Info:</h3>

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

<td valign="top">
	<xsl:for-each select="term">
		<nobr>&quot;<xsl:value-of select="." />&quot;</nobr>
		&#160;
	</xsl:for-each>
	<br />
</td>

<!--
<b>URLs and Alternate Terms:</b><br />
-->

<td valign="top">
	<!-- Suggested URLs -->
	<xsl:for-each select="url">
		&#160; <b>URL:</b> &#160;
		<small><xsl:value-of select="text()" /></small><br />
	</xsl:for-each>

	<!-- Alternative or related Search Terms -->
	<xsl:for-each select="alternate_term">
		&#160; <b>Alt Term:</b> &#160;
		<xsl:value-of select="text()" /><br />
	</xsl:for-each>

	<!-- Advertisements or other user Markups -->
	<xsl:for-each select="user_data_markup_items/item">
		&#160;
		<b>Ad:</b>
		&#160;
		<xsl:value-of select="advertisement_code" />
		&#160;
		&quot;<xsl:value-of select="normalize-space(url/title)" />&quot;
		&#160;
		<small><xsl:value-of select="url/text()" /></small><br />
	</xsl:for-each>


</td>

</tr>

<!--
<p />
-->
</xsl:template>



</xsl:stylesheet>
