debug ?= 0
quiet := @
sbt_image_version ?= 8u232_1.3.13

ifneq ($(debug),0)
ifneq ($(debug),)
$(info Debug output enabled at level $(debug))
quiet =
endif
endif

# Create a default target
.PHONY: no_targets__
@no_targets__: list

# List the make targets
.PHONY: list
list: 
	$(quiet) echo "List make targets:" &&\
	cat Makefile | grep "^[A-Za-z0-9_-]\+:" | grep -v "^_[A-Za-z0-9_-]\+" | awk '{print $$1}' | sed "s/://g" | sed "s/^/   /g" | sort

clean:
	docker run -it --rm \
	  -v $$PWD:/app \
	  -w /app \
	  mozilla/sbt:$(sbt_image_version) \
	  sbt clean

build:
	docker run -it --rm \
	  -v $$PWD:/app \
	  -w /app \
	  mozilla/sbt:$(sbt_image_version) \
	  /bin/bash -c "sbt compile && sbt test && sbt package" &&\
	sudo chown -R chho.chho target &&\
	targetPath=$$(ls -1 target/scala*/*.jar) &&\
	echo "Plugin JAR file: $${targetPath}"

sbt-shell:
	docker run -it --rm \
	  -v $$PWD:/app \
	  -w /app \
	  mozilla/sbt:$(sbt_image_version) \
	  sbt shell &&\ 
	sudo chown -R chho.chho target
