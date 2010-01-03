<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- <xsl:output method="text"/> -->

<!-- The name of the database target we are using -->
<xsl:param name="db" />

<!-- The root -->
<xsl:template match="/">

<statements>

<!-- Preamble: dropping old versions of the tables, etc -->

<!-- Create fields: the actual create statement, types -->
<xsl:apply-templates select="table" mode="schema" />

<!-- Indicies and constraints -->
<xsl:apply-templates select="table" mode="indicies" />


<!-- closing -->

</statements>

</xsl:template>


<!-- Create fields:
	Including the actual schema, types, etc.
	==========================================
-->
<!-- The Table -->
<!-- ========= -->
<xsl:template match="table" mode="schema" >
<statement>
	CREATE TABLE <xsl:value-of select="@name" />
	( 	<!-- each field -->
		<xsl:apply-templates
			select="field[not(@is_implemented) or (@is_implemented='TRUE')]"
			mode="schema"
		/>
		<!--
			select="field/[(@is_implemented='TRUE') or (@is_implemented='')]"
			select="field[@is_implemented='TRUE']"
		-->
	)
</statement>
</xsl:template>

<!-- Each Field -->
<!-- ========== -->
<xsl:template match="field" mode="schema" >

	<!-- The name of the field -->
	<xsl:value-of select="@name" />

	<!-- It's declaration, dispatch to proper DB -->
	<xsl:choose>
		<!-- Oracle -->
		<xsl:when test="$db = 'oracle'">
			<xsl:apply-templates select="."
				mode="schema_oracle_field_decl"
			/>
		</xsl:when>
		<!-- PostgreSQL -->
		<xsl:when test="$db = 'postgresql'">
			<xsl:apply-templates select="."
				mode="schema_postgresql_field_decl"
			/>
		</xsl:when>
		<!-- MySQL -->
		<xsl:when test="$db = 'mysql'">
			<xsl:apply-templates select="."
				mode="schema_mysql_field_decl"
			/>
		</xsl:when>
		<!-- MS SQL Server -->
		<xsl:when test="$db='sqlserver'">
			<xsl:apply-templates select="."
				mode="schema_sqlserver_field_decl"
			/>
		</xsl:when>
		<!-- Else we have no idea! -->
		<xsl:otherwise>
			otherwise "<xsl:value-of select="$db" />"
			can only do oracle or postgresql
		</xsl:otherwise>
	</xsl:choose>

	<!-- not null -->

	<!-- The closing comma, if needed -->
	<xsl:if test="position() &lt; last()">,
	</xsl:if>
</xsl:template>

<!-- The declaration portion for an ORACLE field -->
<!-- ==============================########===== -->
<xsl:template match="field" mode="schema_oracle_field_decl">
	<xsl:choose>
		<xsl:when test="(@type='boolean') or (@type='int') or (@type='long')">	NUMBER</xsl:when>
		<xsl:when test="@type='text'">	VARCHAR2(<xsl:value-of select="@size" />)</xsl:when>
		<xsl:when test="@type='timestamp'">	DATE</xsl:when>
		<xsl:otherwise>
			UNKNOWN type "<xsl:value-of select="@type" />"
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!-- The declaration portion for an POSTGRES field -->
<!-- ==============================##########===== -->
<xsl:template match="field" mode="schema_postgresql_field_decl">
	<xsl:choose>
		<xsl:when test="(@type='boolean') or (@type='int') or (@type='long')">	INT8</xsl:when>
		<xsl:when test="@type='text'">	TEXT</xsl:when>
		<xsl:when test="@type='timestamp'">	TIMESTAMP</xsl:when>
		<xsl:otherwise>
			UNKNOWN type "<xsl:value-of select="@type" />"
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!-- The declaration portion for an MySQL field -->
<!-- ===============================#####====== -->
<xsl:template match="field" mode="schema_mysql_field_decl">
	<xsl:choose>
		<xsl:when test="@type='boolean'">	TINYINT</xsl:when>
		<xsl:when test="@type='int'">	INT</xsl:when>
		<xsl:when test="@type='long'">	BIGINT</xsl:when>
		<xsl:when test="@type='text'">	VARCHAR(<xsl:value-of select="@size" />)</xsl:when>
		<!-- MySQL also supports DATETIME which is not epoch based -->
		<!--
		<xsl:when test="@type='timestamp'">	TIMESTAMP</xsl:when>
		-->
		<xsl:when test="@type='timestamp'">	DATETIME</xsl:when>
		<xsl:otherwise>
			UNKNOWN type "<xsl:value-of select="@type" />"
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!-- The declaration portion for an SQLSERVER field -->
<!-- ==============================###########===== -->
<xsl:template match="field" mode="schema_sqlserver_field_decl">
	<xsl:choose>
		<xsl:when test="(@type='boolean')">	TINYINT</xsl:when>
		<xsl:when test="(@type='int') or (@type='long')">	NUMERIC(20)</xsl:when>
		<xsl:when test="@type='text'">	VARCHAR(<xsl:value-of select="@size" />)</xsl:when>
		<xsl:when test="@type='timestamp'">	DATETIME</xsl:when>
		<xsl:otherwise>
			UNKNOWN type "<xsl:value-of select="@type" />"
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>



<!-- The Indicies -->
<!-- ============ -->
<xsl:template match="table" mode="indicies" >
	<!-- each field -->
	<xsl:apply-templates
		select="field[ (not(@is_implemented) or (@is_implemented='TRUE')) and @is_indexed='TRUE']"
		mode="indicies"
	/>
</xsl:template>

<!-- Each Field -->
<!-- ========== -->
<xsl:template match="field" mode="indicies" >
	<!--
	<statement>
		<xsl:value-of select="position()" />
	</statement>
	-->

	<xsl:choose>
		<!-- Oracle -->
		<xsl:when test="$db = 'oracle'">
			<xsl:apply-templates select="."
				mode="index_oracle_field"
			>
				<xsl:with-param name="nth" select="position()" />
			</xsl:apply-templates>
		</xsl:when>
		<!-- PostgreSQL -->
		<xsl:when test="$db = 'postgresql'">
			<xsl:apply-templates select="."
				mode="index_postgresql_field"
			/>
		</xsl:when>
		<!-- MySQL -->
		<xsl:when test="$db = 'mysql'">
			<xsl:apply-templates select="."
				mode="index_mysql_field"
			/>
		</xsl:when>
		<!-- Sql Server -->
		<xsl:when test="$db = 'sqlserver'">
			<xsl:apply-templates select="."
				mode="index_sqlserver_field"
			/>
		</xsl:when>
		<!-- Else we have no idea! -->
		<xsl:otherwise>
			otherwise "<xsl:value-of select="$db" />"
			can only do oracle, postgresql or sqlserver
		</xsl:otherwise>
	</xsl:choose>

	<!-- not null -->

	<!-- The closing comma, if needed -->
	<xsl:if test="position() &lt; last()">,
	</xsl:if>
</xsl:template>



<!-- The index for an ORACLE field -->
<!-- ================########===== -->
<xsl:template match="field" mode="index_oracle_field">
	<xsl:param name="nth" />
	<statement>
		CREATE INDEX
			<!--
			NUX_<xsl:value-of select="../@name" />_<xsl:value-of select="@name" />
			nux_foo
			NUX_<xsl:value-of select="../@name" />_F<xsl:value-of select="position()" />
			-->
			NUX_<xsl:value-of select="../@name" />_F<xsl:value-of select="$nth" />
		ON
			<xsl:value-of select="../@name" />( <xsl:value-of select="@name" /> )
	</statement>
</xsl:template>


<!-- The index for a POSTGRESQL field -->
<!-- ===============############===== -->
<xsl:template match="field" mode="index_postgresql_field">
	<statement>
		CREATE INDEX
			&quot;<xsl:value-of select='../@name' />_<xsl:value-of select='@name' />&quot;
		ON
			&quot;<xsl:value-of select='../@name' />&quot;
		USING BTREE
		(
			&quot;<xsl:value-of select="@name" />&quot;

			<xsl:choose>
				<xsl:when test="(@type='boolean') or (@type='int') or (@type='long')">
					&quot;int8_ops&quot;
				</xsl:when>
				<xsl:when test="@type='text'">
					&quot;text_ops&quot;
				</xsl:when>
				<xsl:when test="@type='timestamp'">
					&quot;timestamp_ops&quot;
				</xsl:when>
				<xsl:otherwise>
					&quot;UNKNOWN type <xsl:value-of select="@type" />&quot;
				</xsl:otherwise>
			</xsl:choose>
		)
	</statement>
</xsl:template>

<!-- The index for an MySQL field -->
<!-- ================#######===== -->
<xsl:template match="field" mode="index_mysql_field">
	<statement>
		CREATE INDEX
			NUX_<xsl:value-of select="../@name" />_<xsl:value-of select="@name" />
		ON
			<xsl:value-of select="../@name" />( <xsl:value-of select="@name" /> )
	</statement>
</xsl:template>

<!-- The index for an Sql Server field -->
<!-- ================############===== -->
<xsl:template match="field" mode="index_sqlserver_field">
	<statement>
		CREATE INDEX
			NUX_<xsl:value-of select="../@name" />_<xsl:value-of select="@name" />
		ON
			<xsl:value-of select="../@name" />( <xsl:value-of select="@name" /> )
	</statement>
</xsl:template>



</xsl:stylesheet>
