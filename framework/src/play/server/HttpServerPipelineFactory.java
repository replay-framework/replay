package play.server;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import play.Invoker;

public class HttpServerPipelineFactory implements ChannelPipelineFactory {
    private final Invoker invoker;

    public HttpServerPipelineFactory(Invoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public ChannelPipeline getPipeline() {
        return new PlayChannelPipeline();
    }

    class PlayChannelPipeline extends DefaultChannelPipeline {
        private final PlayHandler playHandler = new PlayHandler(invoker);

        PlayChannelPipeline() {
            addChannelHandler(new FlashPolicyHandler());
            addChannelHandler(new HttpRequestDecoder());
            addChannelHandler(new StreamChunkAggregator());
            addChannelHandler(new HttpResponseEncoder());
            addChannelHandler(new ChunkedWriteHandler());

            addLast("handler", playHandler);
            playHandler.pipelines.put("handler", playHandler);
        }

        private void addChannelHandler(ChannelHandler instance) {
            String name = instance.getClass().getSimpleName();
            addLast(name, instance);
            playHandler.pipelines.put(name, instance);
        }
    }
}

