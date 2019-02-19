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
      // Only implement network capabilities for now.
      case "NET_RAW" => Some(CapabilityInfo.Capability.NET_RAW)
      case "NET_ADMIN" => Some(CapabilityInfo.Capability.NET_ADMIN)
      case "NET_BIND" => Some(CapabilityInfo.Capability.NET_BIND_SERVICE)
      case "NET_BROADCAST" => Some(CapabilityInfo.Capability.NET_BROADCAST)
      case _ => None
    }
  }

  private def extractCapabilitiesFromLabels(builder: TaskInfo.Builder, capabilitiesLabelName: String): Option[CapabilityInfo] = {
    import collection.JavaConverters._

    val capabilitiesLabels = builder.getLabels.getLabelsList.stream()
      .filter(_.getKey.equals(capabilitiesLabelName)).collect(Collectors.toList())

    if (capabilitiesLabels.size() == 0) {
      return None
    }

    if (capabilitiesLabels.size() > 1) {
      logger.info(s"There should be only exactly one label with key $capabilitiesLabelName for " +
        s"task ${builder.getTaskId}")
      return None
    }

    val capabilities = capabilitiesLabels.get(0).getValue.split(";")
      .map(_.trim)
      .map(capabilityStrToCapability)
      .filter(_.isDefined)
      .map(_.get)
      .toSeq.asJava


    Some(CapabilityInfo.newBuilder()
      .addAllCapabilities(capabilities).build())
  }

  override def taskInfo(appSpec: ApplicationSpec,
                        builder: TaskInfo.Builder): Unit = {
    if (!builder.hasLabels) {
      logger.debug(s"No labels for task ${builder.getTaskId}")
      return
    }

    val boundingCapabilitiesBuilder = extractCapabilitiesFromLabels(builder, MESOS_BOUNDING_CAPABILITIES_LABEL)
    val effectiveCapabilitiesBuilder = extractCapabilitiesFromLabels(builder, MESOS_EFFECTIVE_CAPABILITIES_LABEL)

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

