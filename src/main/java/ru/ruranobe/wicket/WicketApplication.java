package ru.ruranobe.wicket;

import net.ftlines.wicketsource.WicketSource;
import org.apache.wicket.authorization.strategies.CompoundAuthorizationStrategy;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AnnotationsRoleAuthorizationStrategy;
import org.apache.wicket.core.request.handler.BookmarkableListenerInterfaceRequestHandler;
import org.apache.wicket.core.request.handler.ListenerInterfaceRequestHandler;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.core.util.file.WebApplicationPath;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebResponse;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.crypt.CachingSunJceCryptFactory;
import org.wicketstuff.pageserializer.kryo.KryoSerializer;
import org.wicketstuff.rest.utils.mounting.PackageScanner;
import ru.ruranobe.misc.RuranobeUtils;
import ru.ruranobe.wicket.webpages.*;

import javax.servlet.http.HttpServletResponse;

public class WicketApplication extends AuthenticatedWebApplication
{

    @Override
    public Class<? extends WebPage> getHomePage()
    {
        return HomePage.class;
    }

    @Override
    public void init()
    {
        super.init();
        getFrameworkSettings().setSerializer(new KryoSerializer());
        WicketSource.configure(this);

        // preload rura configuration
        RuranobeUtils.getApplicationContext();

        getResourceSettings().getResourceFinders().add(
                new WebApplicationPath(getServletContext(), "markupFolder"));

        mountPages();

        getJavaScriptLibrarySettings().setJQueryReference(new ResourceReference("")
        {
            @Override
            public IResource getResource()
            {
                return null;
            }
        });

        getApplicationSettings().setAccessDeniedPage(NotFound.class);

        getRequestCycleSettings().setResponseRequestEncoding("UTF-8");
        getMarkupSettings().setDefaultMarkupEncoding("UTF-8");
        getMarkupSettings().setStripWicketTags(true);
        getSecuritySettings().setAuthenticationStrategy(new RuranobeAuthenticationStrategy("ruranobe_global_up_key"));
        getSecuritySettings().setCryptFactory(new CachingSunJceCryptFactory("randomlyGeneratedRuraCryptoKey"));

        getSecuritySettings().setAuthorizationStrategy(new AnnotationsRoleAuthorizationStrategy(this));
    }

    private void mountPages()
    {
        mount(new MountedMapper("/diary", Diary.class));
        mount(new MountedMapper("/projects", FullProjects.class));
        mount(new MountedMapper("/faq", Faq.class));
        mount(new MountedMapper("/r/${project}", ProjectPage.class));
        mount(new MountedMapper("/r/${project}/${volume}", VolumePage.class));
        mount(new MountedMapper("/updates", Updates.class));
        mount(new MountedMapper("/r/${project}/${volume}/text", Text.class));
        mount(new MountedMapper("/r/${project}/${volume}/${chapter}", Text.class));
        mount(new MountedMapper("/user/cabinet", Cabinet.class));
        mount(new MountedMapper("/user/register", Register.class));
        mount(new MountedMapper("/user/login", LoginPage.class));
        mount(new MountedMapper("/user/recover/pass", PasswordRecoveryPage.class));
        mount(new MountedMapper("/user/recover/pass/email", EmailPasswordRecoveryPage.class));
        mount(new MountedMapper("/user/email/activate", ActivateEmail.class));
        mount(new MountedMapper("/upload/image", UploadImage.class));
        getRootRequestMapperAsCompound().add(new NoVersionMapper("/a/${project}/${volume}/${chapter}", Editor.class));
        getRootRequestMapperAsCompound().add(new NoVersionMapper("/a/${project}/${volume}", VolumeEdit.class));
        getRootRequestMapperAsCompound().add(new NoVersionMapper("/a/${project}", ProjectEdit.class));
        getRootRequestMapperAsCompound().add(new NoVersionMapper("/a", GlobalEdit.class));
        mount(new Orphus.OrphusMountedMapper("/a/orphus/#{project}/#{volume}/#{chapter}", "/a/orphus"));
        mount(new MountedMapper("/notfound", NotFound.class));
        mount(new MountedMapper("/aboutus", AboutUs.class));
        mount(new MountedMapper("/recruit", Recruit.class));
        mount(new MountedMapper("/contact", Contact.class));
        mount(new MountedMapper("/help", Help.class));

        PackageScanner.scanPackage("ru.ruranobe.wicket.resources");
    }

    protected WebResponse newWebResponse(final WebRequest webRequest,
                                         final HttpServletResponse httpServletResponse)
    {
        return new ServletWebResponse((ServletWebRequest) webRequest, httpServletResponse);
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass()
    {
        return LoginSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass()
    {
        return LoginPage.class;
    }

    private static class NoVersionMapper extends MountedMapper
    {
        public NoVersionMapper(final Class<? extends IRequestablePage> pageClass)
        {
            this("/", pageClass);
        }

        public NoVersionMapper(String mountPath, final Class<? extends IRequestablePage> pageClass)
        {
            super(mountPath, pageClass, new PageParametersEncoder());
        }

        @Override
        protected void encodePageComponentInfo(Url url, PageComponentInfo info)
        {
            //Does nothing
        }

        @Override
        public Url mapHandler(IRequestHandler requestHandler)
        {
            if (requestHandler instanceof ListenerInterfaceRequestHandler || requestHandler instanceof BookmarkableListenerInterfaceRequestHandler)
            {
                return null;
            }
            else
            {
                return super.mapHandler(requestHandler);
            }
        }
    }
}
