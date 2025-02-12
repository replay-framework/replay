package ui.hello;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import hello.fasttags.HelloFromCore;
import hello.fasttags.HelloFromCoreGT;
import tags.fasttags.HelloFromApp;
import tags.fasttags.HelloFromAppGT;

import play.template2.GTFastTag;
import play.templates.FastTags;

import org.junit.jupiter.api.Test;

public class MultiModuleAppGotRenderStaticSpec extends BaseSpec {
  @Test
  public void openHelloWorldPage() {
    open("/");

    $("h1#fasttag_hello_app").shouldHave(text("Hello from "),
        text(HelloFromApp.class.getAnnotation(FastTags.Namespace.class).value()));
    $("h1#fasttag_hello_core").shouldHave(text("Hello from "),
        text(HelloFromCore.class.getAnnotation(FastTags.Namespace.class).value()));

    $("h1#gt_hello_app").shouldHave(text("Hello from "),
        text(HelloFromAppGT.class.getAnnotation(GTFastTag.TagNamespace.class).value()));
    $("h1#gt_hello_core").shouldHave(text("Hello from "),
        text(HelloFromCoreGT.class.getAnnotation(GTFastTag.TagNamespace.class).value()));
  }
}
