plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "io.github.lemcoder"
version = "1.0.0"

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.fleeksoft.io.core)
                implementation(libs.fleeksoft.io.io)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jvmMain.dependencies {
            implementation("org.codehaus.plexus:plexus-xml:4.0.4") // Actual JVM implementation
        }

        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
            implementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
        }
    }
}

tasks.withType<Test> {
    maxHeapSize = "4g"
    forkEvery = 1
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    useJUnitPlatform() // Enable JUnit 5
}






