package com.criteo.marathon

import mesosphere.marathon.plugin.ApplicationSpec
import org.apache.mesos.Protos._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec

class CapabilitiesPluginSpec extends FunSpec with MockFactory {
  import collection.JavaConverters._

  describe("CapabilitiesPluginSpec") {
    describe("ContainerInfo") {
      it("should force container type to MESOS if no value is provided by Marathon") {
        val plugin = new CapabilitiesPlugin
        val appSpec = mock[ApplicationSpec]
        val taskInfoBuilder = TaskInfo.newBuilder

        taskInfoBuilder.setCommand(CommandInfo.newBuilder()
          .setValue("./a_command"))
        taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
          .setKey("MESOS_BOUNDING_CAPABILITIES")
          .setValue("NET_RAW"))

        taskInfoBuilder.getContainerBuilder.clearType()
        plugin.taskInfo(appSpec, taskInfoBuilder)

        assert(taskInfoBuilder.getContainer.getType == ContainerInfo.Type.MESOS)
      }
    }

    describe("LinuxInfo") {
      it("should not be set if no labels are provided") {
        val plugin = new CapabilitiesPlugin
        val appSpec = mock[ApplicationSpec]
        val taskInfoBuilder = TaskInfo.newBuilder

        taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))

        plugin.taskInfo(appSpec, taskInfoBuilder)

        assert(!taskInfoBuilder.getContainer.hasLinuxInfo)
      }

      it("should not be set if no capabilities are provided") {
        val plugin = new CapabilitiesPlugin
        val appSpec = mock[ApplicationSpec]
        val taskInfoBuilder = TaskInfo.newBuilder

        taskInfoBuilder.setCommand(CommandInfo.newBuilder()
          .setValue("./a_command"))
        taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
          .setKey("MYLABEL")
          .setValue("test"))

        plugin.taskInfo(appSpec, taskInfoBuilder)

        assert(!taskInfoBuilder.getContainer.hasLinuxInfo)
      }

      it("should be set if bounding capabilities are provided") {
        val plugin = new CapabilitiesPlugin
        val appSpec = mock[ApplicationSpec]
        val taskInfoBuilder = TaskInfo.newBuilder

        taskInfoBuilder.setCommand(CommandInfo.newBuilder()
          .setValue("./a_command"))
        taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
          .setKey("MESOS_BOUNDING_CAPABILITIES")
          .setValue("NET_RAW"))

        plugin.taskInfo(appSpec, taskInfoBuilder)

        assert(taskInfoBuilder.getContainer.hasLinuxInfo)
      }

      it("should be set if effective capabilities are provided") {
        val plugin = new CapabilitiesPlugin
        val appSpec = mock[ApplicationSpec]
        val taskInfoBuilder = TaskInfo.newBuilder

        taskInfoBuilder.setCommand(CommandInfo.newBuilder()
          .setValue("./a_command"))
        taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
          .setKey("MESOS_EFFECTIVE_CAPABILITIES")
          .setValue("NET_RAW"))

        plugin.taskInfo(appSpec, taskInfoBuilder)

        assert(taskInfoBuilder.getContainer.hasLinuxInfo)
      }

      describe("Bounding capabilities") {
        it("should contain one bounding capability") {
          val plugin = new CapabilitiesPlugin
          val appSpec = mock[ApplicationSpec]
          val taskInfoBuilder = TaskInfo.newBuilder

          taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))
          taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
            .setKey("MESOS_BOUNDING_CAPABILITIES")
            .setValue("NET_RAW"))

          plugin.taskInfo(appSpec, taskInfoBuilder)

          assert(taskInfoBuilder.getContainer.getLinuxInfo.getBoundingCapabilities.getCapabilitiesList == List(
            CapabilityInfo.Capability.NET_RAW
          ).asJava)
        }

        it("should contain two bounding capabilities") {
          val plugin = new CapabilitiesPlugin
          val appSpec = mock[ApplicationSpec]
          val taskInfoBuilder = TaskInfo.newBuilder

          taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))
          taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
            .setKey("MESOS_BOUNDING_CAPABILITIES")
            .setValue("NET_RAW;NET_ADMIN"))

          plugin.taskInfo(appSpec, taskInfoBuilder)

          assert(taskInfoBuilder.getContainer.getLinuxInfo.getBoundingCapabilities.getCapabilitiesList == List(
            CapabilityInfo.Capability.NET_RAW,
            CapabilityInfo.Capability.NET_ADMIN
          ).asJava)
        }
      }

      describe("Effective capabilities") {
        it("should contain one effective capability") {
          val plugin = new CapabilitiesPlugin
          val appSpec = mock[ApplicationSpec]
          val taskInfoBuilder = TaskInfo.newBuilder

          taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))
          taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
            .setKey("MESOS_EFFECTIVE_CAPABILITIES")
            .setValue("NET_RAW"))

          plugin.taskInfo(appSpec, taskInfoBuilder)

          assert(taskInfoBuilder.getContainer.getLinuxInfo.getEffectiveCapabilities.getCapabilitiesList == List(
            CapabilityInfo.Capability.NET_RAW
          ).asJava)
        }

        it("should contain two effective capabilities") {
          val plugin = new CapabilitiesPlugin
          val appSpec = mock[ApplicationSpec]
          val taskInfoBuilder = TaskInfo.newBuilder

          taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))
          taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
            .setKey("MESOS_EFFECTIVE_CAPABILITIES")
            .setValue("NET_RAW;NET_ADMIN"))

          plugin.taskInfo(appSpec, taskInfoBuilder)

          assert(taskInfoBuilder.getContainer.getLinuxInfo.getEffectiveCapabilities.getCapabilitiesList == List(
            CapabilityInfo.Capability.NET_RAW,
            CapabilityInfo.Capability.NET_ADMIN
          ).asJava)
        }
      }

      describe("Capabilities") {
        it("should trim spaces from label value") {
          val plugin = new CapabilitiesPlugin
          val appSpec = mock[ApplicationSpec]
          val taskInfoBuilder = TaskInfo.newBuilder

          taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))
          taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
            .setKey("MESOS_BOUNDING_CAPABILITIES")
            .setValue("   NET_RAW  ;   NET_ADMIN  "))

          plugin.taskInfo(appSpec, taskInfoBuilder)

          assert(taskInfoBuilder.getContainer.getLinuxInfo.getBoundingCapabilities.getCapabilitiesList == List(
            CapabilityInfo.Capability.NET_RAW,
            CapabilityInfo.Capability.NET_ADMIN
          ).asJava)
        }

        it("should not crash on unknown capabilities") {
          val plugin = new CapabilitiesPlugin
          val appSpec = mock[ApplicationSpec]
          val taskInfoBuilder = TaskInfo.newBuilder

          taskInfoBuilder.setCommand(CommandInfo.newBuilder()
            .setValue("./a_command"))
          taskInfoBuilder.getLabelsBuilder.addLabels(Label.newBuilder()
            .setKey("MESOS_BOUNDING_CAPABILITIES")
            .setValue("CUSTOM_CAPABILITY;UNKNOWN_CAPA"))

          plugin.taskInfo(appSpec, taskInfoBuilder)

          assert(taskInfoBuilder.getContainer.getLinuxInfo.getBoundingCapabilities.getCapabilitiesList == List().asJava)
        }
      }
    }
  }
}
