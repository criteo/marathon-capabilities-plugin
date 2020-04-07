package com.criteo.marathon

import java.util.stream.Collectors

import com.typesafe.scalalogging.StrictLogging
import mesosphere.marathon.plugin.plugin.PluginConfiguration
import mesosphere.marathon.plugin.task._
import mesosphere.marathon.plugin.{ApplicationSpec, PodSpec}
import org.apache.mesos.Protos._
import play.api.libs.json.JsObject

class CapabilitiesPlugin extends RunSpecTaskProcessor with PluginConfiguration with StrictLogging
{
  private val MESOS_BOUNDING_CAPABILITIES_LABEL = "MESOS_BOUNDING_CAPABILITIES"
  private val MESOS_EFFECTIVE_CAPABILITIES_LABEL = "MESOS_EFFECTIVE_CAPABILITIES"

  override def initialize(marathonInfo: Map[String, Any],
    configuration: JsObject): Unit = {
  }

  private def capabilityStrToCapability(capabilityStr: String): Option[CapabilityInfo.Capability] = {
    capabilityStr match {
      case "AUDIT_CONTROL" => Some(CapabilityInfo.Capability.AUDIT_CONTROL)
      case "AUDIT_READ" => Some(CapabilityInfo.Capability.AUDIT_READ)
      case "AUDIT_WRITE" => Some(CapabilityInfo.Capability.AUDIT_WRITE)
      case "BLOCK_SUSPEND" => Some(CapabilityInfo.Capability.BLOCK_SUSPEND)
      case "CHOWN" => Some(CapabilityInfo.Capability.CHOWN)
      case "DAC_OVERRIDE" => Some(CapabilityInfo.Capability.DAC_OVERRIDE)
      case "DAC_READ_SEARCH" => Some(CapabilityInfo.Capability.DAC_READ_SEARCH)
      case "FOWNER" => Some(CapabilityInfo.Capability.FOWNER)
      case "FSETID" => Some(CapabilityInfo.Capability.FSETID)
      case "IPC_LOCK" => Some(CapabilityInfo.Capability.IPC_LOCK)
      case "IPC_OWNER" => Some(CapabilityInfo.Capability.IPC_OWNER)
      case "KILL" => Some(CapabilityInfo.Capability.KILL)
      case "LEASE" => Some(CapabilityInfo.Capability.LEASE)
      case "LINUX_IMMUTABLE" => Some(CapabilityInfo.Capability.LINUX_IMMUTABLE)
      case "MAC_ADMIN" => Some(CapabilityInfo.Capability.MAC_ADMIN)
      case "MAC_OVERRIDE" => Some(CapabilityInfo.Capability.MAC_OVERRIDE)
      case "MKNOD" => Some(CapabilityInfo.Capability.MKNOD)
      case "NET_ADMIN" => Some(CapabilityInfo.Capability.NET_ADMIN)
      case "NET_BIND_SERVICE" => Some(CapabilityInfo.Capability.NET_BIND_SERVICE)
      case "NET_BROADCAST" => Some(CapabilityInfo.Capability.NET_BROADCAST)
      case "NET_RAW" => Some(CapabilityInfo.Capability.NET_RAW)
      case "SETFCAP" => Some(CapabilityInfo.Capability.SETFCAP)
      case "SETGID" => Some(CapabilityInfo.Capability.SETGID)
      case "SETPCAP" => Some(CapabilityInfo.Capability.SETPCAP)
      case "SETUID" => Some(CapabilityInfo.Capability.SETUID)
      case "SYS_ADMIN" => Some(CapabilityInfo.Capability.SYS_ADMIN)
      case "SYS_BOOT" => Some(CapabilityInfo.Capability.SYS_BOOT)
      case "SYS_CHROOT" => Some(CapabilityInfo.Capability.SYS_CHROOT)
      case "SYSLOG" => Some(CapabilityInfo.Capability.SYSLOG)
      case "SYS_MODULE" => Some(CapabilityInfo.Capability.SYS_MODULE)
      case "SYS_NICE" => Some(CapabilityInfo.Capability.SYS_NICE)
      case "SYS_PACCT" => Some(CapabilityInfo.Capability.SYS_PACCT)
      case "SYS_PTRACE" => Some(CapabilityInfo.Capability.SYS_PTRACE)
      case "SYS_RAWIO" => Some(CapabilityInfo.Capability.SYS_RAWIO)
      case "SYS_RESOURCE" => Some(CapabilityInfo.Capability.SYS_RESOURCE)
      case "SYS_TIME" => Some(CapabilityInfo.Capability.SYS_TIME)
      case "SYS_TTY_CONFIG" => Some(CapabilityInfo.Capability.SYS_TTY_CONFIG)
      case "WAKE_ALARM" => Some(CapabilityInfo.Capability.WAKE_ALARM)
      case _ => None
    }
  }

  private def extractCapabilitiesFromLabels(builder: TaskInfo.Builder, capabilitiesLabelName: String)
    : (Seq[String], Option[CapabilityInfo]) = {
    import collection.JavaConverters._

    val capabilitiesLabels = builder.getLabels.getLabelsList.stream()
      .filter(_.getKey.equals(capabilitiesLabelName)).collect(Collectors.toList())

    if (capabilitiesLabels.size() == 0) {
      return (Seq(), None)
    }

    if (capabilitiesLabels.size() > 1) {
      logger.info(s"There should be only exactly one label with key $capabilitiesLabelName for " +
        s"task ${builder.getTaskId}")
      return (Seq(), None)
    }

    val capabilities = capabilitiesLabels.get(0).getValue.split(",")
      .map(_.trim)
      .map(c => (c, capabilityStrToCapability(c))).toSeq

    val unknownCapabilitiesStr = capabilities.filter(_._2.isEmpty).map(_._1)

    val filteredCapabilities = capabilities
      .filter(_._2.isDefined)
      .map(_._2.get)
      .asJava

    (unknownCapabilitiesStr, Some(CapabilityInfo.newBuilder()
      .addAllCapabilities(filteredCapabilities).build()))
  }

  override def taskInfo(appSpec: ApplicationSpec,
                        builder: TaskInfo.Builder): Unit = {
    if (!builder.hasLabels) {
      logger.debug(s"No labels for task ${builder.getTaskId}")
      return
    }

    val (unknownBoundingCapabilities, boundingCapabilitiesBuilder) = extractCapabilitiesFromLabels(
      builder, MESOS_BOUNDING_CAPABILITIES_LABEL)
    val (unknownEffectiveCapabilities, effectiveCapabilitiesBuilder) = extractCapabilitiesFromLabels(
      builder, MESOS_EFFECTIVE_CAPABILITIES_LABEL)

    if (unknownBoundingCapabilities.nonEmpty || unknownEffectiveCapabilities.nonEmpty) {
      val unknownCapabilities = unknownBoundingCapabilities.union(unknownEffectiveCapabilities).toSet.mkString(", ")
      builder.getCommandBuilder.setValue(
        s"echo 'Unknown capabilities: $unknownCapabilities. Please double check your configuration.' >&2 && exit 1")
      return
    }

    if (boundingCapabilitiesBuilder.isEmpty && effectiveCapabilitiesBuilder.isEmpty) {
      logger.debug(s"No special capabilities required for task ${builder.getTaskId}")
      return
    }

    var linuxInfoBuilder = builder.getContainerBuilder.getLinuxInfoBuilder

    linuxInfoBuilder = boundingCapabilitiesBuilder
      .map(linuxInfoBuilder.setBoundingCapabilities(_))
      .getOrElse(linuxInfoBuilder)

    linuxInfoBuilder = effectiveCapabilitiesBuilder
        .map(linuxInfoBuilder.setEffectiveCapabilities(_))
        .getOrElse(linuxInfoBuilder)

    var containerBuilder = builder.getContainerBuilder

    if (!containerBuilder.hasType)
      containerBuilder = containerBuilder.setType(ContainerInfo.Type.MESOS)

    containerBuilder
      .setLinuxInfo(linuxInfoBuilder)
  }

  override def taskGroup(podSpec: PodSpec, executor: ExecutorInfo.Builder,
    taskGroup: TaskGroupInfo.Builder): Unit = {
    // Do not handle taskGroup for now.
  }
}

