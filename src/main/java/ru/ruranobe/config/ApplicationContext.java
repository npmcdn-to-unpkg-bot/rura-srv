package ru.ruranobe.config;

import org.dom4j.xpath.*;
import org.dom4j.*;
import ru.ruranobe.engine.Webpage;
import ru.ruranobe.engine.files.FileStorageService;
import ru.ruranobe.engine.image.ImageStorage;

import java.util.*;

public class ApplicationContext
{
    ApplicationContext(Document document)
    {
        loadFileStorageServiceConfig(document);
        loadWebPageConfig(document);
    }

    private void loadWebPageConfig(Document document)
    {
        XPath xpath = new DefaultXPath("/Configuration/Webpages/Webpage");
        List<Element> webpages = xpath.selectNodes(document);
        if (webpages != null)
        {
            for (Element webpage : webpages)
            {
                Webpage.Builder webpageBuilder = new Webpage.Builder();
                String pageClass = webpage.elementText(WEBPAGE_PAGE_CLASS);
                webpageBuilder.setPageClass(pageClass);
                List<Element> imageStorages = webpage.selectNodes(WEBPAGE_IMAGE_STORAGE);
                for (Element imageStorage : imageStorages)
                {
                    String serviceName = imageStorage.elementText(IMAGE_STORAGE_SERVICE_NAME);
                    String storagePath = imageStorage.elementText(IMAGE_STORAGE_STORAGE_PATH);
                    webpageBuilder.addImageStorage(new ImageStorage(serviceName, storagePath));
                }
                pageClassToWebpage.put(pageClass, webpageBuilder.build());
            }
        }
    }

    private void loadFileStorageServiceConfig(Document document)
    {
        XPath xpath = new DefaultXPath("/Configuration/FileStoragePolicy/FileStorageService");
        List<Element> fileStorageServices = xpath.selectNodes(document);
        if (fileStorageServices != null)
        {
            for (Element fileStorageService : fileStorageServices)
            {
                String serviceName = fileStorageService.elementText(FILE_STORAGE_SERVICE_NAME);
                /*String login = fileStorageService.elementText(FILE_STORAGE_LOGIN);
                String password = fileStorageService.elementText(FILE_STORAGE_PASSWORD);*/
                String refreshToken = fileStorageService.elementText(FILE_STORAGE_REFRESH_TOKEN);
                String clientId = fileStorageService.elementText(FILE_STORAGE_CLIENTID);
                String clientSecret = fileStorageService.elementText(FILE_STORAGE_CLIENT_SECRET);
                String accessToken = fileStorageService.elementText(FILE_STORAGE_ACCESS_TOKEN);
                /*List<String> fileExtensions = new ArrayList<String>();
                List<Element> fileExtensionsDom = fileStorageService.selectNodes(FILE_STORAGE_FILE_EXTENSION);
                for (Element fileExtensionDom : fileExtensionsDom)
                {
                    fileExtensions.add(fileExtensionDom.getText());
                }*/

                this.fileStorageServices.add(new FileStorageService.Builder()
                        .setServiceName(serviceName)
                        /*.setLogin(login)
                        .setPassword(password)
                        .setFileExtension(fileExtensions)*/
                        .setRefreshToken(refreshToken)
                        .setClientId(clientId)
                        .setClientSecret(clientSecret)
                        .setAccessToken(accessToken)
                        .build());
            }
        }
    }

    public Webpage getWebpageByPageClass(String pageClass)
    {
        return pageClassToWebpage.get(pageClass);
    }

    private Map<String, Webpage> pageClassToWebpage = new HashMap<String, Webpage>();
    private List<FileStorageService> fileStorageServices = new ArrayList<FileStorageService>();
    private static final String WEBPAGE_PAGE_CLASS = "PageClass";
    private static final String WEBPAGE_IMAGE_STORAGE = "ImageStorage";
    private static final String IMAGE_STORAGE_SERVICE_NAME = "ServiceName";
    private static final String IMAGE_STORAGE_STORAGE_PATH = "StoragePath";
    private static final String FILE_STORAGE_CLIENTID = "ClientId";
    private static final String FILE_STORAGE_CLIENT_SECRET = "ClientSecret";
    private static final String FILE_STORAGE_SERVICE_NAME = "ServiceName";
    private static final String FILE_STORAGE_REFRESH_TOKEN = "RefreshToken";
    private static final String FILE_STORAGE_ACCESS_TOKEN = "AccessToken";
    /*private static final String FILE_STORAGE_LOGIN = "Login";
    private static final String FILE_STORAGE_PASSWORD = "Password";
    private static final String FILE_STORAGE_FILE_EXTENSION = "FileExtension";*/
}