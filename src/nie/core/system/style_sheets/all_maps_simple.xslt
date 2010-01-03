<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<xsl:param name="object" />

<xsl:template match="/">
<html>
<head>
<title>Monitor</title>
<!--
<meta http-equiv="Refresh" content="300" />
-->
</head>
<body>
	<!--
	<h2>Monitor</h2>
	<p/>
	-->

	<TABLE border="0">

	<TR>
	<TD colspan="3">
	<CENTER>
	<h3>Monitor: Featured Processor / <i>Queue</i></h3>
	<xsl:apply-templates select="status/processors/processor_status[@name=$object]" mode="detailed" />
	<xsl:apply-templates select="status/queues/queue_status[@name=$object]" mode="detailed" />
	</CENTER>
	</TD>
	</TR>

	<!-- 2nd line, Spacer -->
	<TR><TD>&#160;</TD></TR>


	<!-- Third line, two colomn table of procs and queues -->
	<TR>

	<TD valign="top">
	<CENTER>
	<B>Processors</B>
	<table>

	<!-- Fill in the rows (and header above row 1 ) -->
	<xsl:apply-templates select="status/processors/processor_status" mode="single_table_row" />


	</table>
	</CENTER>
	</TD>


	<!-- Spacer -->
	<TD>&#160;</TD>

	<TD valign="top">
	<CENTER>
	<B><i>Queues</i></B>
	<table>

	<!-- Fill in the rows ( and the header above row 1 ) -->
	<xsl:apply-templates select="status/queues/queue_status" mode="single_table_row" />

	</table>
	</CENTER>
	</TD>

	</TR>
	</TABLE>

</body>
</html>
</xsl:template> <!-- End of root template -->


	<!-- PROCESSOR Display
	     ================================
		Several levels of detail depending on "mode"
	-->

	<!-- Detailed processor -->
	<xsl:template match="processor_status" mode="detailed" >

	<table border="0">
	<tr>

	<!-- feeders -->
	<td rowspan="2">
		<!-- reads_from -->
		<xsl:apply-templates select="connections/reads_from" mode="feeding_parent_queues" />
	</td>

	<td rowspan="2">&#160;</td>

	<!-- Details of Processor -->
	<td bgcolor="#eeeeee">
	Processor:
	<b><xsl:value-of select="@name" /></b>
	<p/>
	<small>
	Activity:
	<xsl:value-of select="dequeue_information/number_operations" />
		/
	<xsl:value-of select="enqueue_information/number_operations" />
	<br/>
	State:
	<xsl:value-of select="state" />
	<br/>
	Last wakeup:
	<xsl:value-of select="dequeue_information/since_last_woke_up" />
		ago
	<br/>
	Can_exit:
	<xsl:value-of select="can_exit" />
	Pri:
	<xsl:value-of select="current_priority" />

	<!-- Parameters: -->
	<xsl:if test="count(parameters) > 0" >
	<table bgcolor="#dddddd">
	<tr><td>
	<font size="1"><pre><xsl:value-of select="parameters"/></pre></font>
	</td></tr>
	</table>
	</xsl:if>

	Todo: add timing info
	</small>
	</td>

	<td rowspan="2">&#160;</td>

	<!-- consumers -->
	<td rowspan="2">
		<!-- writes_to -->
		<xsl:apply-templates select="connections/writes_to" mode="consuming_child_queues" />
	</td>

	</tr>

	<!-- users -->
	<xsl:if test="count(connections/uses) > 0" >
	<tr>
	<td>
		<!--small-->
		<i>
		<b>uses:</b>
		<!--br/-->
		<xsl:apply-templates select="connections/uses" />
		</i>
		<!--/small-->
	</td>
	</tr>
	</xsl:if>

	</table>

	</xsl:template>



	<xsl:template match="processor_status" mode="single_table_row">

	<xsl:if test="position() = 1">
		<tr valign="bottom" bgcolor="#ccddff">
		<th>Name</th>
		<th>Activity</th>
		<th>Pri</th>
		<th><font size="1">Last Dequeue<br/>(how long ago)</font></th>
		<!--
		<th><font size="1">Status</font></th>
		-->
		<th><font size="1">Can<br/>Exit</font></th>
		</tr>
	</xsl:if>

		<tr>
			<!-- Shade the rows -->
			<xsl:choose>
				<xsl:when test="position() mod 2 = 0">
					<xsl:attribute name="bgcolor">white</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="bgcolor">#eeeeee</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>

		<!-- Name -->
		<td>
			<xsl:call-template name="object_name_and_link">
				<xsl:with-param name="object_name" select="@name" />
			</xsl:call-template>
		</td>


		<!-- Activity -->
		<td align="center">
		<xsl:value-of select="dequeue_information/number_operations" />
		/
		<xsl:value-of select="enqueue_information/number_operations" />
		</td>

		<!-- Priority -->
		<td align="center">
		<font>
		<xsl:choose>
			<xsl:when test="current_priority &gt; 5">
				<xsl:attribute name="color">green</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="current_priority &lt; 5">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">-1</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="current_priority" />
		</font>
		</td>

		<!-- How long ago was the last dequeue -->
		<td align="center">

		<!--
		test:
		<xsl:if test="count(dequeue_information/last_woke_up) &gt; 1">
		<xsl:call-template name="display_duration_time">
			<xsl:with-param name="time_ms" select="dequeue_information/since_last_woke_up/@ms" />
			<xsl:with-param name="time_display" select="dequeue_information/since_last_woke_up" />
		</xsl:call-template>
		</xsl:if>
		other test passing node
		<xsl:call-template name="display_duration_time">
			<xsl:with-param name="time_node" select="dequeue_information/since_last_woke_up" />
		</xsl:call-template>
		-->
		<font>
		<xsl:choose>
			<xsl:when test="dequeue_information/since_last_woke_up/@ms &gt; 60000">
				<xsl:attribute name="color">red</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="dequeue_information/since_last_woke_up/@ms &gt; 5000">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">-0</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="dequeue_information/since_last_woke_up" />
		</font>
		</td>

		<!-- Status -->
		<!--
		<td valign="center">
		<small>
		<xsl:value-of select="state" />
		</small>
		</td>
		-->


		<!-- Can Exit -->
		<td align="center">
		<xsl:value-of select="can_exit" />
		</td>


		</tr>

	</xsl:template>


	<!-- QUEUES
	     ====================
	-->


	<!-- Detailed view of a Queue -->

	<xsl:template match="queue_status" mode="detailed" >

	<table border="0">
	<tr>

	<!-- feeders -->
	<td rowspan="2">
		<!-- reads_from -->
		<xsl:apply-templates select="connections/reads_from" mode="feeding_parent_processors" />
	</td>

	<td bgcolor="#eeeeee">
	<i>
	Queue:
	<b><xsl:value-of select="@name" /></b>
	</i>
	<p/>
	<small>
	Activity:
	<xsl:value-of select="enqueue_information/number_operations" />
		/
	<xsl:value-of select="items_in_queue" />
		/
	<xsl:value-of select="dequeue_information/number_operations" />
	<br/>
	Can_exit:
	<xsl:value-of select="can_exit" />
	<p/>
	Todo: add timing info, parameters?, class
	</small>
	</td>

	<!-- consumers -->
	<td rowspan="2">
		<!-- writes_to -->
		<xsl:apply-templates select="connections/writes_to" mode="consuming_child_processors" />
	</td>

	</tr>

	<!-- users -->
	<xsl:if test="count(connections/uses) > 0" >
	<tr>
	<td>
		<!--small-->
		<b>uses:</b>
		<!--br/-->
		<xsl:apply-templates select="connections/uses" />
		<!--/small-->
	</td>
	</tr>
	</xsl:if>

	</table>
	</xsl:template>


	<!-- Single table row Queue entry -->
	<xsl:template match="queue_status" mode="single_table_row">

		<!-- Add the header row above the first row -->
		<xsl:if test="position() = 1">
		<tr valign="bottom" bgcolor="#ccddff">
		<th><i>Name</i></th>
		<th>Activity<br/>
		<font size="1">enqueues / size / dequeues</font>
		</th>
		<th><font size="1">Last Enqueue<br/>(how long ago)</font></th>
		<th><font size="1">Last Dequeue<br/>(how long ago)</font></th>
		<!--
		<th>Pri</th>
		<th><font size="1">Status</font></th>
		-->
		<th><font size="1">Can<br/>Exit</font></th>
		</tr>
		</xsl:if>

		<tr>
			<!-- Shade the rows -->
			<xsl:choose>
				<xsl:when test="position() mod 2 = 0">
					<xsl:attribute name="bgcolor">white</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="bgcolor">#eeeeee</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>

		<!-- Name -->
		<td>
		<i>
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="@name" />
		</xsl:call-template>
		</i>
		</td>

		<!-- Activity -->
		<td align="center">
		<font>
		<xsl:choose>
			<xsl:when test="items_in_queue &gt; 10">
				<xsl:attribute name="color">red</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="items_in_queue &gt; 0">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">+0</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="enqueue_information/number_operations" />
			/
		<xsl:value-of select="items_in_queue" />
			/
		<xsl:value-of select="dequeue_information/number_operations" />
		</font>
		</td>

		<!-- How long ago was the last enqueue -->
		<td align="center">

		<font>
		<xsl:choose>
			<xsl:when test="enqueue_information/since_last_operation/@ms &gt; 60000">
				<xsl:attribute name="color">red</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="enqueue_information/since_last_operation/@ms &gt; 5000">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">-0</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="enqueue_information/since_last_operation" />
		</font>
		</td>
		<!-- How long ago was the last dequeue -->
		<td align="center">

		<font>
		<xsl:choose>
			<xsl:when test="dequeue_information/since_last_operation/@ms &gt; 60000">
				<xsl:attribute name="color">red</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="dequeue_information/since_last_operation/@ms &gt; 5000">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">-0</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="dequeue_information/since_last_operation" />
		</font>
		</td>
		<!--
		<td/>
		<td/>
		-->

		<!-- Can Exit -->
		<td align="center">
		<xsl:value-of select="can_exit" />
		</td>

		</tr>
	</xsl:template>

	<!-- Generally a reference to an object by name -->
	<xsl:template match="object">
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="." />
		</xsl:call-template>
		<br/>
	</xsl:template>

	<!-- Direct parents and children, queues/processors -->

	<!-- A Processor has Parent Queues (Q's that feed data to it) -->
	<xsl:template match="object" mode="feeding_parent_queues">
		<table border="0">
		<tr>
		<td>
		<!-- Display the grandparents -->
		<xsl:call-template name="feeding_grandparent_processors">
			<xsl:with-param name="grandparents_child_queue" select="." />
		</xsl:call-template>
		</td>
		<td>
		<nobr>
		<!-- Display this parent queue -->
		<i>
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="." />
		</xsl:call-template>
		</i>
		<!-- Display the connecting line -->
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="1" />
		</xsl:call-template>
		</nobr>
		</td>
		</tr>
		</table>
	</xsl:template>

	<!-- A Processor has Child Queues (Q's that consume data from it) -->
	<xsl:template match="object" mode="consuming_child_queues">
		<table border="0">
		<tr>
		<td>
		<!-- Display the connecting line -->
		<nobr>
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="2" />
		</xsl:call-template>
		<!-- Display this child queue -->
		<i>
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="." />
		</xsl:call-template>
		</i>
		</nobr>
		</td>
		<td>
		<!-- Display the grandchildren -->
		<xsl:call-template name="consuming_grandchild_processors">
			<xsl:with-param name="grandchilds_parent_queue" select="." />
		</xsl:call-template>
		</td>
		</tr>
		</table>
	</xsl:template>

	<!-- A Queue has Parent Processors (Proc's that feed data to it) -->
	<xsl:template match="object" mode="feeding_parent_processors">
		<table border="0">
		<tr>
		<td>
		<!-- Display the grandparents -->
		<xsl:call-template name="feeding_grandparent_queues">
			<xsl:with-param name="grandparents_child_processor" select="." />
		</xsl:call-template>
		</td>
		<td>
		<nobr>
		<!-- Display this parent processor -->
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="." />
		</xsl:call-template>
		<!-- Display the connecting line -->
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="1" />
		</xsl:call-template>
		</nobr>
		</td>
		</tr>
		</table>
	</xsl:template>

	<!-- A Queue has Child Processors (Procs's that consume data from it) -->
	<xsl:template match="object" mode="consuming_child_processors">
		<table border="0">
		<tr>
		<td>
		<!-- Display the connecting line -->
		<nobr>
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="2" />
		</xsl:call-template>
		<!-- Display this child processor -->
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="." />
		</xsl:call-template>
		</nobr>
		</td>
		<td>
		<!-- Display the grandchildren -->
		<xsl:call-template name="consuming_grandchild_queues">
			<xsl:with-param name="grandchilds_parent_processor" select="." />
		</xsl:call-template>
		</td>
		</tr>
		</table>
	</xsl:template>


	<!-- Displaying 2 levels up/down, Grandparents and Grandchildren -->


	<!-- Processors have Procs as grandchildren, via an intermediate Q -->
	<xsl:template name="consuming_grandchild_processors">
		<xsl:param name="grandchilds_parent_queue" />
		<xsl:apply-templates select="/status/processors/processor_status[ $grandchilds_parent_queue = connections/reads_from/object ]" mode="short_consumer" />
	</xsl:template>


	<!-- Processors have Procs as grandparents, via an intermediate Q -->
	<xsl:template name="feeding_grandparent_processors">
		<xsl:param name="grandparents_child_queue" />
		<xsl:apply-templates select="/status/processors/processor_status[ $grandparents_child_queue = connections/writes_to/object ]" mode="short_feeder" />
		<!--xsl:for-each select="connections/reads_from/object"-->
		<!--
		Param1: <xsl:value-of select="$grandparents_child_queue" />
		<xsl:apply-templates select="/status/processors/processor_status/connections/writes_to/object" mode="short" />
		-->
	</xsl:template>


	<!-- Queues have Q's as grandchildren, via an intermediate Proc -->
	<xsl:template name="consuming_grandchild_queues">
		<xsl:param name="grandchilds_parent_processor" />
		<xsl:apply-templates select="/status/queues/queue_status[ $grandchilds_parent_processor = connections/reads_from/object ]" mode="short_consumer" />
	</xsl:template>


	<!-- Queues have Q's as grandparents, via an intermediate Proc -->
	<xsl:template name="feeding_grandparent_queues">
		<xsl:param name="grandparents_child_processor" />
		<xsl:apply-templates select="/status/queues/queue_status[ $grandparents_child_processor = connections/writes_to/object ]" mode="short_feeder" />
	</xsl:template>


	<!-- Display of individual grandchildren or grandparents -->


	<xsl:template match="processor_status" mode="short_feeder">
		<nobr>
		<!-- Display the name and link -->
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="@name" />
		</xsl:call-template>
		<!-- Display the connecting arrow -->
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="1" />
		</xsl:call-template>
		</nobr>
		<br/>
	</xsl:template>


	<xsl:template match="queue_status" mode="short_feeder">
		<nobr>
		<!-- Display the name and link -->
		<i>
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="@name" />
		</xsl:call-template>
		</i>
		<!-- Display the connecting arrow -->
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="1" />
		</xsl:call-template>
		</nobr>
		<br/>
	</xsl:template>


	<xsl:template match="processor_status" mode="short_consumer">
		<!-- Display the connecting arrow -->
		<nobr>
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="2" />
		</xsl:call-template>
		<!-- Display the name and link -->
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="@name" />
		</xsl:call-template>
		</nobr>
		<br/>
	</xsl:template>

	<xsl:template match="queue_status" mode="short_consumer">
		<!-- Display the connecting arrow -->
		<nobr>
		<xsl:call-template name="connecting_line">
			<xsl:with-param name="position" select="position()" />
			<xsl:with-param name="count" select="last()" />
			<xsl:with-param name="direction" select="2" />
		</xsl:call-template>
		<!-- Display the name and link -->
		<i>
		<xsl:call-template name="object_name_and_link">
			<xsl:with-param name="object_name" select="@name" />
		</xsl:call-template>
		</i>
		</nobr>
		<br/>
	</xsl:template>


	<!-- SUBROUTINES
	     =========================
	-->

	<xsl:template name="object_name_and_link">
		<xsl:param name="object_name" />
		<font>
		<xsl:if test="string-length($object_name) > 20" >
			<xsl:attribute name="size">
				1
			</xsl:attribute>
		</xsl:if>
		<a>
			<xsl:attribute name="href">
				view?object=<xsl:value-of select="$object_name"/>
			</xsl:attribute>
			<xsl:value-of select="$object_name" />
		</a>
		</font>
	</xsl:template>

	<xsl:template name="connecting_line">
		<xsl:param name="position" />
		<xsl:param name="count" />
		<xsl:param name="direction" />
		<!--
		Pos: <xsl:value-of select="$position" />
		Count: <xsl:value-of select="$count" />
		-->
		<!-- Which type of connecting line to display -->
		<xsl:choose>
			<!-- Special case if there's only a single object -->
			<xsl:when test="$count &lt; 2">
				-&gt;
			</xsl:when>
			<!-- Else we have multiple objects -->
			<xsl:otherwise>
				<xsl:choose>
					<!-- Top half of list -->
					<xsl:when test="$position &lt;= $count div 2">
						<!-- Which "direction" are we going? -->
						<xsl:choose>
							<!-- Feeder -->
							<xsl:when test="$direction = 1">
								\
							</xsl:when>
							<!-- Consumer -->
							<xsl:when test="$direction = 2">
								/
							</xsl:when>
							<!-- Unknown -->
							<xsl:otherwise>
								-
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<!-- Bottom half of list -->
					<xsl:when test="$position &gt; $count div 2 + 0.6">
						<!-- Which "direction" are we going? -->
						<xsl:choose>
							<xsl:when test="$direction = 1">
								/
							</xsl:when>
							<xsl:when test="$direction = 2">
								\
							</xsl:when>
							<xsl:otherwise>
								-
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<!-- Exact dead center of list (with odd # elems) -->
					<xsl:otherwise>
						--
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise> <!-- End else we have multiple objects -->
		</xsl:choose>
	</xsl:template>

			<xsl:with-param name="time_ms" select="dequeue_information/since_last_woke_up/@ms" />
			<xsl:with-param name="time_display" select="dequeue_information/since_last_woke_up" />





	<!-- Nice formatting of time duration, how long AGO -->
	<!--
	<xsl:template name="display_duration_time">
		<xsl:if test="count($time_ms) &gt; 0" >
		<xsl:param name="time_ms" />
		<xsl:param name="time_display" />
		ms:
		<xsl:value-of select="$time_ms" />
		display:
		<xsl:value-of select="$time_display" />
		<xsl:if test="$time_ms &gt; 60000">
			long time
		</xsl:if>
		<font>
		<xsl:choose>
			<xsl:when test="$time_ms &gt; 60000">
				<xsl:attribute name="color">red</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="$time_ms &gt; 5000">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">-0</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$time_display" />
		</font>
		</xsl:if>
	</xsl:template>
	-->

	<!--
	<xsl:template name="display_duration_time">
		<xsl:param name="time_node" />
		<font>
		<xsl:choose>
			<xsl:when test="$time_node/@ms &gt; 60000">
				<xsl:attribute name="color">red</xsl:attribute>
				<xsl:attribute name="size">+1</xsl:attribute>
			</xsl:when>
			<xsl:when test="$time_node/@ms &gt; 5000">
				<xsl:attribute name="color">brown</xsl:attribute>
				<xsl:attribute name="size">-0</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="color">black</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="time_node" />
		</font>
	</xsl:template>
	-->

</xsl:stylesheet>
