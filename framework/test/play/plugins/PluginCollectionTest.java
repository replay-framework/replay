package play.plugins;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static play.ClasspathResource.file;

import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import play.Play;
import play.PlayBuilder;
import play.PlayPlugin;
import play.data.parsing.TempFilePlugin;
import play.data.validation.ValidationPlugin;
import play.db.DBPlugin;
import play.db.jpa.JPAPlugin;
import play.i18n.MessagesPlugin;
import play.jobs.JobsPlugin;
import play.libs.WS;

public class PluginCollectionTest {

  @BeforeEach
  public void init() {
    new PlayBuilder().build();
  }

  @Test
  public void verifyLoading() {
    PluginCollection pc = new PluginCollection();
    Play.configuration.setProperty("play.plugins.descriptor", "play.plugins.sample");

    pc.loadPlugins();

    // the following plugin-list should match the list in the file 'play.plugins'
    assertThat(pc.getEnabledPlugins())
        .containsExactly(
            pc.getPluginInstance(TempFilePlugin.class),
            pc.getPluginInstance(ValidationPlugin.class),
            pc.getPluginInstance(DBPlugin.class),
            pc.getPluginInstance(JPAPlugin.class),
            pc.getPluginInstance(MessagesPlugin.class),
            pc.getPluginInstance(WS.class),
            pc.getPluginInstance(JobsPlugin.class),
            pc.getPluginInstance(PlayStatusPlugin.class));
  }

  @Test
  public void verifyLoadingFromFilesWithBlankLines() {
    // create custom PluginCollection that fakes that TestPlugin is application plugin
    PluginCollection pc = new PluginCollection();
    // make sure we load custom play.plugins-file
    Play.configuration.setProperty(
        "play.plugins.descriptor", "play/plugins/custom-play-with-blank-lines.plugins");

    pc.loadPlugins();

    PlayStatusPlugin playStatusPlugin_first_instance = pc.getPluginInstance(PlayStatusPlugin.class);
    TestPlugin testPlugin_first_instance = pc.getPluginInstance(TestPlugin.class);

    assertThat(pc.getAllPlugins())
        .containsExactly(playStatusPlugin_first_instance, testPlugin_first_instance);
  }

  /**
   * Avoid including the same class+index twice.
   *
   * <p>This happened in the past under a range of circumstances, including: Class path on NTFS or
   * other case-insensitive file system includes "play.plugins" directory 2 times.
   */
  @Test
  public void skipsDuplicatePlugins() {
    PluginCollection pc = spy(new PluginCollection());
    doReturn(
            asList(
                file("play/plugins/custom-play.plugins"),
                file("play/plugins/custom-play.plugins.duplicate")))
        .when(pc)
        .getPlayPluginFiles();
    pc.loadPlugins();
    assertThat(pc.getAllPlugins())
        .containsExactly(
            pc.getPluginInstance(PlayStatusPlugin.class), pc.getPluginInstance(TestPlugin.class));
  }

  @Test
  public void canLoadPlayPluginsFromASingleDescriptor() {
    Play.configuration.setProperty("play.plugins.descriptor", "play/plugins/custom-play.plugins");
    PluginCollection pc = new PluginCollection();
    assertThat(pc.getPlayPluginFiles().size()).isEqualTo(1);
    assertThat(pc.getPlayPluginFiles().get(0).toString())
        .endsWith("play/plugins/custom-play.plugins");
  }

  @Test
  public void canLoadPlayPluginsFromMultipleDescriptors() {
    Play.configuration.setProperty(
        "play.plugins.descriptor", "play/plugins/custom-play.plugins,play.plugins.sample");
    PluginCollection pc = new PluginCollection();
    assertThat(pc.getPlayPluginFiles().size()).isEqualTo(2);
    assertThat(pc.getPlayPluginFiles().get(0).toString())
        .endsWith("play/plugins/custom-play.plugins");
    assertThat(pc.getPlayPluginFiles().get(1).toString()).endsWith("play.plugins.sample");
  }

  @Test
  public void reversedListOfPlugins() {
    Play.configuration.setProperty("play.plugins.descriptor", "play/plugins/custom-play.plugins");
    PluginCollection pc = new PluginCollection();
    pc.loadPlugins();

    List<PlayPlugin> reversedPlugins = pc.getReversedEnabledPlugins().collect(toList());
    assertThat(reversedPlugins).hasSize(2);
    assertThat(reversedPlugins.get(0).getClass()).isEqualTo(TestPlugin.class);
    assertThat(reversedPlugins.get(1).getClass()).isEqualTo(PlayStatusPlugin.class);
  }

  @Test
  public void reversedListOfPlugins_forEmptyList() {
    Play.configuration.setProperty("play.plugins.descriptor", "play/plugins/missing-file");
    PluginCollection pc = new PluginCollection();
    pc.loadPlugins();

    assertThat(pc.getReversedEnabledPlugins()).hasSize(0);
  }

  @Test
  public void onApplicationStop_runsPluginsInReverseOrder() {
    PluginCollection pc = new PluginCollection();
    PlayPlugin plugin1 = spy(new TestPlugin());
    PlayPlugin plugin2 = spy(new FooPlugin());
    PlayPlugin plugin3 = spy(new BarPlugin());
    pc.addPlugin(plugin1);
    pc.addPlugin(plugin2);
    pc.addPlugin(plugin3);

    pc.onApplicationStop();

    InOrder inOrder = inOrder(plugin1, plugin2, plugin3);
    inOrder.verify(plugin3).onApplicationStop();
    inOrder.verify(plugin2).onApplicationStop();
    inOrder.verify(plugin1).onApplicationStop();
  }

  @Test
  public void loadPlugins_shouldAllowDirectInvocation() {
    new PluginCollection().loadPlugins(new TreeSet<>());
  }
}

class TestPlugin extends PlayPlugin {
  {
    index = 1;
  }
}

class FooPlugin extends PlayPlugin {
  {
    index = 2;
  }
}

class BarPlugin extends PlayPlugin {
  {
    index = 3;
  }
}
