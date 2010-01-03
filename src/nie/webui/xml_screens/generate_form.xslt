<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<!-- Variables that will be passed in to us -->
<xsl:param name="title" />
<xsl:param name="submit_link" />
<xsl:param name="image_dir" />
<xsl:param name="help_dir" />
<!--
<xsl:param name="password" />
-->
<!-- July 2008 using obscured passwords now -->
<xsl:param name="s" />
<xsl:param name="operation" />


<xsl:template match="/form">
<html>

<head>
<!--
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="nie.css" />
-->
<title>
<!--
<xsl:value-of select="@title" />
-->
<xsl:value-of select="$title" />
</title>
</head>

<body>
<center>

<h1 class="nie_form_title">
	<!--
	<xsl:value-of select="@title" />
	-->

	<!-- These should line up with constants in class UILink -->
	<xsl:if test="$operation = 'add'">
		Create
	</xsl:if>
	<xsl:if test="$operation = 'edit'">
		Edit
	</xsl:if>
	<xsl:if test="$operation = 'delete'">
		Confirm Deleition of
	</xsl:if>
	<xsl:if test="$operation = 'view'">
		Viewing
	</xsl:if>


	<xsl:value-of select="$title" />
</h1>

<!-- Start the form outside the main table -->
<form>
<xsl:attribute name="action">
	<xsl:value-of select="$submit_link" />
</xsl:attribute>

<!--
<input type="hidden" name="password">
	<xsl:attribute name="value">
		<xsl:value-of select="$password" />
	</xsl:attribute>
</input>
-->
<!-- July '08 changing from password in cleartext
	to scrambled key, sort of a session id
-->
<input type="hidden" name="s">
	<xsl:attribute name="value">
		<xsl:value-of select="$s" />
	</xsl:attribute>
</input>
<input type="hidden" name="operation">
	<xsl:attribute name="value">
		<xsl:value-of select="$operation" />
	</xsl:attribute>
</input>

<!-- Overall Table -->
<table _border="1" class="nie_main_content_table" cellpadding="2" cellspacing="2">

	<!-- Display an error or other message, if there is one -->
	<xsl:if test="@message">
		<tr>
		<td align="center" colspan="3">
			<!-- Add CSS class if requested -->
			<xsl:if test="@severity">
				<xsl:attribute name="class">
					nie_form_<xsl:value-of select="@severity" />_msg_cell
				</xsl:attribute>
			</xsl:if>
			<!-- The actual message -->
			<xsl:value-of select="@message" />
		</td>
		</tr>
	</xsl:if>

	<!-- The main sections of the form -->
	<xsl:apply-templates select="section" />

	<xsl:if test="$operation = 'add' or $operation = 'edit'">
		<xsl:if test="//field/@is_required = 'TRUE'">
			<tr>
			<td align="center" colspan="3">
			<sup>* indicates a required field</sup>
			</td>
			</tr>
		</xsl:if>
	</xsl:if>

</table>

    

<!-- Add in the hidden fields -->
<xsl:apply-templates select="//field[(@type='hidden')]" />

</form>

</center>
</body>
</html>
</xsl:template>




<!-- This template copies over XHTML tags we embed in the individual events
     ======================================================================
-->
<xsl:template match="node() | @*">
	<xsl:copy>
		<xsl:apply-templates select="node() | @*"/>
	</xsl:copy>
</xsl:template>


<!-- Sections
====================
-->

<!-- For each SECTION -->
<xsl:template match="section">
	<!-- Use a name and shade the section, if specified
		or just use an HR tag
	-->
	<xsl:choose>
		<!-- If title or help, do the fancy shaded bar -->
		<xsl:when test="@title | @help">
		
			<!-- If not an empty section -->
			<xsl:if test="normalize-space(field/@label) or normalize-space(field/@name) or normalize-space(field)">

				<tr>
				<xsl:attribute name="class">nie_form_header_row</xsl:attribute>
				<td colspan="3">
					<table width="100%" cellpadding="0" cellspacing="0">
					<tr>
					<!-- Section Heading -->
					<xsl:if test="@title">
						<th align="left" class="nie_form_header_cell">
							<xsl:value-of select="@title" />
						</th>
					</xsl:if>
					<!-- Help Link -->
					<xsl:if test="@help">
	
						<td align="right" _class="nie_form_header_cell">
							<a target="_blank">
								<xsl:attribute name="onclick">ReportView = window.open('<xsl:value-of select="$help_dir" /><xsl:value-of select="@help" />','ReportView','scrollbars,resizable,width=350,height=450');ReportView.focus();return false</xsl:attribute>
								<xsl:attribute name="href">
									<xsl:value-of select="$help_dir" />
									<xsl:value-of select="@help" />
								</xsl:attribute>
								<img alt="Click for Popup Help" width="16" height="16" hspace="3" vspace="2" border="0">
									<xsl:attribute name="src"><xsl:value-of select="$image_dir" />help-button-small.gif</xsl:attribute>
								</img>
								<!--
								help
								-->
							</a>
						</td>
	
						<!-- as one line
						<td align="right" _class="nie_form_header_cell"><a target="_blank"><xsl:attribute name="onclick">ReportView = window.open('<xsl:value-of select="$help_dir" /><xsl:value-of select="@help" />','ReportView','scrollbars,resizeable,width=300,height=400');ReportView.focus();return false</xsl:attribute><xsl:attribute name="href"><xsl:value-of select="$help_dir" /><xsl:value-of select="@help" /></xsl:attribute><img alt="Click for Popup Help" width="16" height="16" hspace="3" vspace="2" border="0"><xsl:attribute name="src"><xsl:value-of select="$image_dir" />help-button-small.gif</xsl:attribute></img></a></td>
						-->
	
	
					</xsl:if>
					</tr>
					</table>
				</td>
				</tr>

			</xsl:if>

		</xsl:when>
		<!-- Else just an HR tag -->
		<xsl:otherwise>

			<tr>
			<td colspan="3" class="nie_hr_cell">
				<hr width="100%" size="1" noshade="1" />
			</td>
			</tr>

		</xsl:otherwise>
	</xsl:choose>

	<!-- Add the fields and buttons
		buttons always come after fields in this version
	-->
	<!--
	<xsl:apply-templates select="field" />
	-->
	<!-- Non-hidden fields -->
	<!--
	<xsl:apply-templates />
	<xsl:apply-templates select="field[not(@type) or not (@type='hidden')]" />
	<xsl:apply-templates select="( field[not(@type) or not (@type='hidden')] ) or label" />
	<xsl:apply-templates select="[ field[not(@type) or not (@type='hidden')] or label ]" />
	<xsl:apply-templates select="field[not(@type) or not (@type='hidden')] or inline_button" />
	-->
	<xsl:apply-templates select="field[not(@type) or not (@type='hidden')]" />

	<tr>
	<td colspan="3" align="right">
		<xsl:apply-templates select="button" />
	</td>
	</tr>

	<!-- Add the spacer -->
	<xsl:if test="normalize-space(field/@label) or normalize-space(field/@name) or normalize-space(.)">
		<tr>
			<td class="nie_spacer_cell" />
		</tr>
	</xsl:if>
</xsl:template>


<!-- For each non-hidden FIELD -->
<!-- <xsl:template match="field[not(@type) or not (@type='hidden')]"> -->
<xsl:template match="field[not(@type) or (not (@type='hidden') and not (@type='inline_button'))]">

<xsl:if test="normalize-space(@label) or normalize-space(@name) or normalize-space(.)">


<tr>


<!-- if the field has a name, render it -->
<xsl:choose>
<xsl:when test="@name">


<!-- The Label Cell -->
<td class="nie_form_label_cell" align="right">
	<nobr>
	<xsl:if test="@label">
		<div>
			<!-- markup if the field is highlighted -->
			<xsl:if test="@is_flagged = 'TRUE'">
				<!--
				<xsl:attribute name="color">red</xsl:attribute>
				-->
				<!-- Add CSS class if requested -->
				<xsl:if test="@severity">
					<xsl:attribute name="class">
						nie_form_label_<xsl:value-of select="@severity" />_text
					</xsl:attribute>
				</xsl:if>
			</xsl:if>

			<!-- The actual label's text -->
			<xsl:value-of select="@label" />:

			<!-- Required asterisk (and red if the field is in error) -->
			<xsl:if test="$operation = 'add' or $operation = 'edit'">
				<xsl:if test="@is_required = 'TRUE'">
					&#160;*
					<!--
					<sup>
		
					*
					</sup>
					-->
				</xsl:if>
			</xsl:if>
		</div>
	</xsl:if>
	<!-- Op: <xsl:value-of select="$operation" /> -->
	</nobr>
</td>

<!-- middle spacer cell-->
<td class="nie_spacer_cell" />


<!-- The input field -->
<td class="nie_form_input_cell" valign="top" align="left">

	<xsl:choose>

	<!-- If we're editing or adding, render a full HTML control -->
	<xsl:when test="$operation = 'add' or $operation = 'edit'">

		<!-- Render the control -->
		<xsl:choose>
	
			<!-- Text Area input box -->
			<xsl:when test="@type = 'textarea'">
	
				<!-- <textarea cols="30"> -->
				<!--
				<textarea>
				-->
				<!-- Highlight our one place holder character if it's there -->
				<textarea onFocus="if(this.value.length &lt; 2) this.select();">
					<xsl:attribute name="name">
						<xsl:value-of select="@name" />
					</xsl:attribute>
					<xsl:attribute name="cols">
						<xsl:choose>
							<xsl:when test="@size">
								<xsl:value-of select="@size" />
							</xsl:when>
							<xsl:otherwise>
								30
							</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="rows">
						<xsl:choose>
							<xsl:when test="@rows">
								<xsl:value-of select="@rows" />
							</xsl:when>
							<xsl:otherwise>
								4
							</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>
					<xsl:value-of select="normalize-space(.)" />
					<xsl:if test="not (normalize-space(.))" >
						<xsl:choose>
						<xsl:when test="$operation = 'add' and (@default)" >
							<xsl:value-of select="@default" />
						</xsl:when>
						<xsl:otherwise>
							&#160;
						</xsl:otherwise>
						</xsl:choose>
					</xsl:if>
				</textarea>
			</xsl:when>
	
			<!-- Drop down, select -->
			<xsl:when test="@type = 'select' or @type = 'dropdown'">
				<select>
					<xsl:attribute name="name">
						<xsl:value-of select="@name" />
					</xsl:attribute>
					
					<!-- And now each option -->
					<xsl:for-each select="option">
						<option>
							<!-- The value attribute, defaults to display text -->
							<xsl:attribute name="value">
								<xsl:choose>
									<xsl:when test="@value">
										<xsl:value-of select="@value" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="normalize-space(.)" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<!-- See if this should be selected -->
							<!--
							<xsl:if test="((@value) and @value=normalize-space(..)) or (not(@value) and normalize-space(.) and normalize-space(.)=normalize-space(..))">
							-->
							<xsl:if test="((@value) and @value=../text()) or (not(@value) and normalize-space(.) and normalize-space(.)=../text())">
								<xsl:attribute name="selected">1</xsl:attribute>
							</xsl:if>
							<!-- And the text to display -->
							<xsl:value-of select="normalize-space(.)" />

							<!-- TODO: Remind us that this is not yet implemented -->
							<xsl:if test="$operation = 'add' and not (normalize-space(.)) and (@default)" >
								DEFAULT NOT IMPLEMENTED (=
								<xsl:value-of select="@default" />
								)
							</xsl:if>


						</option>
					</xsl:for-each>
					<!--
					Debug: [<xsl:value-of select="text()" />]
					-->
				</select>
			</xsl:when>

			<!-- checkbox set -->
			<xsl:when test="@type = 'checkbox_set'">

				<table cellpadding="0" cellspacing="1">
				<tr>

				<!-- fpr each option -->
				<xsl:for-each select="option">

					<!-- If there's at least a value or label -->
					<xsl:if test="( @value and not(@value='') ) or ( normalize-space(.) and not(normalize-space(.)='') )" >

						<td>
						<!--
						<nobr>
						-->
						<input type="checkbox">
							<xsl:attribute name="name">
								<xsl:value-of select="../@name" />
							</xsl:attribute>
						
							<!-- The value attribute, defaults to display text -->
							<xsl:attribute name="value">
								<xsl:choose>
									<xsl:when test="@value">
										<xsl:value-of select="@value" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="normalize-space(.)" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
	
							<!-- See if this should be selected -->
							<!--
							<xsl:if test="((@value) and @value=normalize-space(..)) or (not(@value) and normalize-space(.) and normalize-space(.)=normalize-space(..))">
							-->
							<!--
							<xsl:if test="((@value) and @value=../text()) or (not(@value) and normalize-space(.) and normalize-space(.)=../text())">
								<xsl:attribute name="checked">1</xsl:attribute>
							</xsl:if>
							-->
	
							<xsl:if test="@selected = 'TRUE'">
								<xsl:attribute name="checked">1</xsl:attribute>
							</xsl:if>
						</input>
						</td>
	
	
						<!-- The option's label -->
						<td>
						<!-- And the text to display -->
						<xsl:value-of select="normalize-space(.)" />

						<!-- TODO: Remind us that this is not yet implemented -->
						<xsl:if test="$operation = 'add' and not (normalize-space(.)) and (@default)" >
							DEFAULT NOT IMPLEMENTED (=
							<xsl:value-of select="@default" />
							)
						</xsl:if>

						<!--
						&#160;
						&#160;
						</nobr>
						-->
						</td>
	
						<!--
						Debug: [<xsl:value-of select="text()" />]
						-->
	
						<!-- Add spacer colunns and TR breaks -->
						<xsl:choose>
							<!-- if not final cell, force column in between, except for last cell -->
							<xsl:when test="position() mod 3">
								<td>
								<nobr>
								&#160;
								&#160;
								</nobr>
								</td>
							</xsl:when>
							<!-- else is final cell, force a line break after every 2nd/3rd option -->
							<xsl:otherwise>
								<tr />
							</xsl:otherwise>
						</xsl:choose>
	
	
						<!-- force column in between, except for last cell -->
						<!--
						<xsl:if test="position() mod 3">
							<td>
							<nobr>
							&#160;
							</nobr>
							</td>
						</xsl:if>
						-->
	
						<!-- force a line break after every 3rd option -->
						<!--
						<xsl:if test="not (position() mod 3)">
						-->
							<!-- <br/> -->
						<!--
							<tr />
						</xsl:if>
						-->

					</xsl:if> <!-- end if there's at least a value or label -->


				</xsl:for-each>

				</tr>
				</table>

			</xsl:when>


			<!-- Hidden fields handled in other section -->



			<!-- Display-only / Constant fields -->
			<xsl:when test="@type = 'constant'">
				<!-- show the value -->
				<i><b>

					<xsl:if test="text()">
						<xsl:value-of select="normalize-space(.)" />
						<!--
						<xsl:value-of select="text()" />
						-->
					</xsl:if>
					<xsl:if test="$operation = 'add' and not (normalize-space(.))" >
						<xsl:value-of select="@default" />
					</xsl:if>

					&#160;

				</b></i>

				<!-- We actually do want the value to be passed -->
				<input type="hidden">
					<xsl:attribute name="name">
						<xsl:value-of select="@name" />
					</xsl:attribute>
					<xsl:if test="normalize-space(.)">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space(.)" />
						</xsl:attribute>
					</xsl:if>
					<xsl:if test="$operation = 'add' and not (normalize-space(.)) and (@default)">
						<xsl:attribute name="value">
							<xsl:value-of select="@default" />
						</xsl:attribute>
					</xsl:if>
				</input>
			</xsl:when>





	
			<!-- Default is a text box (or password) -->
			<xsl:otherwise>
	
				<!-- <input size="40"> -->
				<input>
					<xsl:attribute name="name">
						<xsl:value-of select="@name" />
					</xsl:attribute>

					<!-- Maybe a password -->
					<xsl:if test="@type = 'password'">
						<!-- Needs to be on ONE LINE for IE -->
						<xsl:attribute name="type">password</xsl:attribute>
					</xsl:if>

					<xsl:attribute name="size">
						<xsl:choose>
							<xsl:when test="@size">
								<xsl:value-of select="@size" />
							</xsl:when>
							<!-- Needs to be on ONE LINE for IE -->
							<xsl:otherwise>40</xsl:otherwise>
						</xsl:choose>
					</xsl:attribute>

					<xsl:if test="@max_input_length">
						<xsl:attribute name="maxlength">
							<xsl:value-of select="@max_input_length" />
						</xsl:attribute>
					</xsl:if>

					<xsl:if test="normalize-space(.)">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space(.)" />
						</xsl:attribute>
					</xsl:if>
					<xsl:if test="$operation = 'add' and not (normalize-space(.)) and (@default)" >
						<xsl:attribute name="value">
							<xsl:value-of select="@default" />
						</xsl:attribute>
					</xsl:if>
				</input>
	
			</xsl:otherwise>
		</xsl:choose>

	</xsl:when>

	<!-- Else we are not rendering a control, just displaying data -->
	<xsl:otherwise>
		<table><tr><td width="300" bgcolor="#f1f1f1">

		<!-- Render the view-only control -->
		<xsl:choose>

		<!-- checkbox set -->
		<xsl:when test="@type = 'checkbox_set'">

			<table cellpadding="0" cellspacing="1">
			<tr>

			<!-- fpr each option -->
			<xsl:for-each select="option">


				<!-- If there's at least a value or label -->
				<xsl:if test="( @value and not(@value='') ) or ( normalize-space(.) and not(normalize-space(.)='') )" >





					<td align="right">
	
						<nobr>
			
							<small><i>
			
							<!--
							[
							<xsl:choose>
								<xsl:when test="@selected = 'TRUE'">
									*
								</xsl:when>
								<xsl:otherwise>
									&#160;
								</xsl:otherwise>
								</xsl:choose>
								]
								-->
				
								<!--
								<xsl:choose>
								<xsl:when test="@selected = 'TRUE'">
									[*]
								</xsl:when>
								<xsl:otherwise>
									[&#160;]
								</xsl:otherwise>
								</xsl:choose>
								-->
				
								<xsl:choose>
								<xsl:when test="@selected = 'TRUE'">
									[YES]
								</xsl:when>
								<xsl:otherwise>
									[no]
								</xsl:otherwise>
							</xsl:choose>
			
							</i></small>
			
						</nobr>
	
					</td>
	
					<!-- The option's label -->
					<td>
						<nobr>	
							<small><i>
			
							<!-- And the text to display -->
							<xsl:value-of select="normalize-space(.)" />
			
							</i></small>
						</nobr>
					</td>
	
					<!-- Add spacer colunns and TR breaks -->
					<xsl:choose>
						<!-- if not final cell, force column in between, except for last cell -->
						<xsl:when test="position() mod 3">
							<td>
							<nobr>
							&#160;
							&#160;
							</nobr>
							</td>
						</xsl:when>
						<!-- else is final cell, force a line break after every 2nd/3rd option -->
						<xsl:otherwise>
							<tr />
						</xsl:otherwise>
					</xsl:choose>

				</xsl:if> <!-- end if there's at least a value or label -->

			</xsl:for-each>

			</tr>
			</table>

		</xsl:when>

		<!-- don't show password fields -->
		<xsl:when test="@type = 'password'">
			<small><i>
				(password field - not shown)
				&#160;
			</i></small>
		</xsl:when>		

		<!-- all other control types -->
		<xsl:otherwise>

		<small><i>
		<!--
		<xsl:value-of select="normalize-space(.)" />
		-->
		<xsl:value-of select="text()" />
		&#160;
		</i></small>

		</xsl:otherwise>
		</xsl:choose>


		</td></tr></table>
	</xsl:otherwise>

	</xsl:choose>


	<!-- Asterisk was here, but had issues with vertical alignment and tall controls -->

</td>

</xsl:when>

<!-- Else the field has no name, so it's just a marker latel -->
<xsl:otherwise>
<td align="center" colspan="2">
<i>
		<xsl:value-of select="@label" />
</i>
</td>
</xsl:otherwise>

</xsl:choose>


</tr>

</xsl:if>

<!-- End for each non-hidden field -->
</xsl:template>


<!-- For each non-hidden FIELD -->
<xsl:template match="label">
<tr>
</tr>
</xsl:template>


<!-- For each hidden FIELD -->
<xsl:template match="field[(@type='hidden')]">
<!--
<xsl:template match="field">
-->
	<!--
	<i><xsl:value-of select="@name" /></i><br/>
	-->
	<input type="hidden">
		<xsl:attribute name="name">
			<xsl:value-of select="@name" />
		</xsl:attribute>
		<xsl:if test="normalize-space(.)">
			<xsl:attribute name="value">
				<xsl:value-of select="normalize-space(.)" />
			</xsl:attribute>
		</xsl:if>
		<!-- Or default -->
		<xsl:if test="$operation = 'add' and not (normalize-space(.)) and (@default)" >
			<xsl:attribute name="value">
				<xsl:value-of select="@default" />
			</xsl:attribute>
		</xsl:if>

	</input>
</xsl:template>







<!-- For each inline_button field -->
<xsl:template match="field[(@type='inline_button')]">
<!-- Only show buttons on Add or Edit, and only for the first URL -->
<xsl:if test="position() &lt;= 5 and ($operation='add' or $operation='edit')">
	<tr>

	<!-- The blank Label Cell -->
	<td class="nie_form_label_cell" align="right" />

	<!-- middle spacer cell-->
	<td class="nie_spacer_cell" />
	
	<!-- The button field -->
	<td class="nie_form_input_cell" valign="top" align="right">
		<nobr>
		<input type="submit" name="button">
			<!--
			<xsl:attribute name="name">
				<xsl:value-of select="@name" />
			</xsl:attribute>
			-->
			<xsl:attribute name="value">
				<xsl:value-of select="normalize-space(.)" />
			</xsl:attribute>

			<!-- Do the popup -->
			<xsl:if test="normalize-space(.) = 'Check URL &gt;'">
				<xsl:attribute name="onclick">PageView = window.open(url_01.value,'PageView','scrollbars,location,resizable,width=800,height=600');PageView.focus();return true;</xsl:attribute>
			</xsl:if>

		</input>
		&#160;
		&#160;
		&#160;
		</nobr>
	</td>

	</tr>
</xsl:if>
</xsl:template>








<!-- Foreach Button in this section (all in one big cell) -->
<xsl:template match="button">
	<xsl:if test="(not(@operation) and not(@skip_operation)) or ((@operation) and (@operation=$operation)) or ((@skip_operation) and not(@skip_operation=$operation))">
		<input type="submit" name="button">
			<xsl:attribute name="value">
				<xsl:value-of select="normalize-space(.)" />
			</xsl:attribute>
		</input>
	</xsl:if>
</xsl:template>


</xsl:stylesheet>
