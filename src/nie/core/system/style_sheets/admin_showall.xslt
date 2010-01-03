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


<!-- MAPPING TABLE -->
<!-- ============= -->
<table border="1">
<tr>
<th width="30%" rowspan="2">Term(s)</th>
<th rowspan="2">Alt Term(s)</th>
<th colspan="6">URL(s)</th>
</tr>
<tr>
<th><small>type</small></th>
<th><small>advertisement_code</small></th>
<th><small>user_class</small></th>
<th>href_url</th>
<th>title</th>
<th>description</th>
</tr>

<!-- xsl:apply-templates select="search_names/fixed_redirection_map/map" / -->
<xsl:apply-templates select="search_tuning/fixed_redirection_map/map" />
</table>

</body>
</html>
</xsl:template> <!-- End of root template -->



<!-- A single map entry -->
<!-- ================== -->

<xsl:template match="map">

<!--
<big><b>Map</b></big><br />
<b>Terms:</b>
-->

<tr>

<!-- Terms -->
<td valign="top">
	<xsl:for-each select="term">
		<!--
		<nobr>&quot;<xsl:value-of select="." />&quot;</nobr>
		&#160;
		-->
		<xsl:value-of select="." /><xsl:if test="position() &lt; last()">,

		</xsl:if>
	</xsl:for-each>
	<br />
</td>

<!-- Alt terms -->
<td valign="top">
	<!-- Alternative or related Search Terms -->
	<xsl:for-each select="alternate_term">
		<!--
		&#160; <b>Alt Term:</b> &#160;
		<xsl:value-of select="text()" /><br />
		-->
		<xsl:value-of select="text()" /><xsl:if test="position() &lt; last()">,

		</xsl:if>
	</xsl:for-each>
</td>

</tr>


<!--
<b>URLs and Alternate Terms:</b><br />
-->
<!-- URLs -->

	<!-- Suggested URLs -->
	<xsl:for-each select="url">

		<tr>
		<td colspan="2" />


		<!-- type -->
		<td valign="top">
		<xsl:choose>
			<xsl:when test="@redirect='1'">
				<font color="red"><b>2</b></font>
			</xsl:when>
			<xsl:otherwise>
				1
			</xsl:otherwise>
		</xsl:choose>
		</td>

		<td/> <!-- No Ad code -->
		<td/> <!-- No Ad class -->

		<!-- href -->
		<td valign="top">
			<small>
				<xsl:value-of select="text()" />
			</small>
		</td>

		<!-- title -->
		<td valign="top">
			<small>
				<xsl:value-of select="title/text()" />
			</small>
		</td>

		<!-- desc -->
		<td valign="top">
			<small>
				<xsl:value-of select="normalize-space(description)" />
			</small>
		</td>


		</tr>

	</xsl:for-each>



<!-- Ads -->
<!-- === -->
	<!-- Advertisements or other user Markups -->
<xsl:for-each select="user_data_markup_items/item">
	
	<tr>
	<td colspan="2" />

	<td valign="top"><font color="green"><b>3</b></font></td>

	<td valign="top">
		<small><xsl:value-of select="advertisement_code" /></small>
	</td>

	<td valign="top"><small>text_ad</small></td>

	<td valign="top">
		<small><xsl:value-of select="url/text()" /></small><br />
	</td>

		<!-- title -->
		<td valign="top">
			<small>
			<xsl:value-of select="normalize-space(url/title)" />
			</small>
		</td>

	<!-- desc -->
	<td valign="top">
	<small>
	<xsl:for-each select="url/description_line">
			<xsl:value-of select="normalize-space(.)" /><xsl:if test="position() &lt; last()">[BR]</xsl:if>
	</xsl:for-each>
	</small>
	</td>

	</tr>

</xsl:for-each>


<!--
<p />
-->
</xsl:template>

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



</xsl:stylesheet>
