<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<!-- Variables that will be passed in to us -->
<xsl:param name="css_text" />

<xsl:template match="/report">
<html>
	<head>
		<style type="text/css">
			<xsl:comment>
/* nie_checkup.css */
<xsl:value-of select="$css_text" />
			</xsl:comment>
		</style>
		<title>
			Collection Report
		</title>
	</head>
	<body>
		<h2 class="nie_report_title">
			Collection Report<br/>
			<small>
				Run on
				<xsl:value-of select="@run_date" />
			</small>
		</h2>

		<!-- Overall Table -->
		<!--
		<table border="0" class="nie_report_table" cellpadding="2" cellspacing="2">
		-->
		<table border="0" class="nie_report_table" cellpadding="0" cellspacing="0">
		
			<!-- The main sections of the form -->
			<xsl:apply-templates select="statistic" />
		
		</table>

	</body>
</html>
</xsl:template>


<!-- For each statistic -->
<xsl:template match="statistic">
	<tr>
		<th align="right" valign="top">
			<xsl:attribute name="class">
				<xsl:value-of select="@class" />
			</xsl:attribute>
			<xsl:value-of select="@label" />
		</th>

		<xsl:apply-templates select="data_element" />

	</tr>
</xsl:template>


<!-- For each non-hidden data item -->
<xsl:template match="data_element">
	<td align="left">
		<xsl:attribute name="class">
			<xsl:value-of select="@class" />
		</xsl:attribute>
		<!--
		<xsl:value-of select="normalize-space(.)" />
		<xsl:apply-templates />
		<xsl:copy>
			<xsl:apply-templates select="node() | @*"/>
		</xsl:copy>
		<xsl:copy />
		-->
		<xsl:copy-of select="node()" />

	</td>
</xsl:template>

<!--
<xsl:template match="br">
	<br/>
</xsl:template>
<xsl:template match="small">
	<small>
		<xsl:apply-templates />
	</small>
</xsl:template>
-->

</xsl:stylesheet>
