package play.plugins;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.PlayPlugin;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurablePluginDisablingPluginTest {

    @Before
    public void before(){
        ConfigurablePluginDisablingPlugin.previousDisabledPlugins.clear();
    }


    @Test
    public void verify_without_config() {

        Properties config = new Properties();
        config.setProperty("some.setting", "some value");

        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );

        internalTest(config, pc, p);
        
    }

    private void internalTest(Properties config, PluginCollection pc, PlayPlugin... correctPluginListAfter) {
        Play.configuration = config;
        Play.pluginCollection = pc;
        ConfigurablePluginDisablingPlugin plugin = new ConfigurablePluginDisablingPlugin();
        plugin.onConfigurationRead();

        assertThat(pc.getEnabledPlugins()).containsOnly(correctPluginListAfter);
    }

    @Test
    public void verify_disabling_plugins() {


        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.setProperty("some.setting", "some value");
        config.setProperty("plugins.disable", "play.plugins.TestPlugin");

        internalTest(config, pc, p2);

    }

    @Test
    public void verify_disableing_many_plugins() {


        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.setProperty("some.setting", "some value");
        config.setProperty("plugins.disable", "play.plugins.TestPlugin");
        config.setProperty("plugins.disable.2", "play.plugins.TestPlugin2");

        internalTest(config, pc);

    }


    @Test
    public void verify_disableing_missing_plugins() {

        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.setProperty("some.setting", "some value");
        config.setProperty("plugins.disable", "play.plugins.TestPlugin_XX");

        internalTest(config, pc, p, p2);

    }

    @Test
    public void verify_re_enabling_disabled_plugins() {


        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.setProperty("some.setting", "some value");
        config.setProperty("plugins.disable", "play.plugins.TestPlugin");

        internalTest(config, pc, p2);

        //remove the disabling from config
        config = new Properties();
        config.setProperty("some.setting", "some value");

        internalTest(config, pc, p, p2);

    }

    @Test
    public void verify_that_the_plugin_gets_loaded(){
        PluginCollection pc = new PluginCollection();
        new PlayBuilder().build();
        Play.configuration.setProperty("play.plugins.descriptor", "play.plugins.sample");

        pc.loadPlugins();

        ConfigurablePluginDisablingPlugin pi = pc.getPluginInstance(ConfigurablePluginDisablingPlugin.class);
        assertThat(pc.getEnabledPlugins()).contains( pi );
    }
}

class TestPlugin extends PlayPlugin {

    //missing constructor on purpose

}

class TestPlugin2 extends PlayPlugin {
    //included constructor on purpose
    public TestPlugin2() {
    }
}