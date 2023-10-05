tasks.named<Jar>("jar") {
    manifest {
        attributes["Plugin-Class"] = "net.dodian.content.plugin.testing.TestingPlugin"
    }
}