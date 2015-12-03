package id.co.ojk.pdfsplitter.controller;

import id.co.ojk.pdfsplitter.model.PreviewResult;
import id.co.ojk.pdfsplitter.model.ResponseResult;
import id.co.ojk.pdfsplitter.service.PDFPreviewService;
import id.co.ojk.pdfsplitter.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/split")
public class SplitterController{
  private static final Logger LOGGER = LoggerFactory.getLogger(SplitterController.class);
  private static final String DF = "yyyyMMdd-HHmmss";
  private static final String PDF_FORMAT = ".pdf";
  
  @Value("${source.folder}")
  private String baseSourceFolder;
  
  @Value("${result.folder}")
  private String baseResultFolder;
  
  @Autowired
  private PDFPreviewService previewService;
  
  @RequestMapping(value="/upload", method=RequestMethod.POST)
  public @ResponseBody PreviewResult upload(@RequestParam("pdf") MultipartFile pdfFile){
    long startTs = System.currentTimeMillis();
    
    PreviewResult result = new PreviewResult();
    if(!pdfFile.isEmpty() && pdfFile.getOriginalFilename().toLowerCase().endsWith(PDF_FORMAT)){
      PDDocument document = null;
      try{
        Date now = new Date();
        
        DateFormat df = new SimpleDateFormat(DF);
        String md5Hex = DigestUtils.md5Hex("" + now.getTime());
        
        String folderName = String.format("%s-%s", df.format(now), md5Hex);
        
        File tempFile = new File(String.format("%s%s%sori%s", baseResultFolder, folderName, File.separator, PDF_FORMAT));
        LOGGER.info("Relocating PDF file to : {}", tempFile.getAbsolutePath());
        tempFile.getParentFile().mkdirs();
        pdfFile.transferTo(tempFile);
        
        document = PDDocument.loadNonSeq(tempFile, null);
        
        String[] previews = previewService.getPreview(document, folderName);
        
        result.setSuccess(1);
        result.setPdfName(folderName);
        result.setImagesUrl(previews);
        LOGGER.info("Processed previews in {} milliseconds", System.currentTimeMillis()-startTs);
      }catch(Exception e){
        LOGGER.error("Error while previewing document [{}] : {}", pdfFile.getName(), e.getMessage());
        result.setErrMsg(String.format("Error while previewing document [%s] : %s", pdfFile.getName(), e.getMessage()));
      }finally{
        if(document != null) try{ document.close(); }catch(IOException e){}
      }
    }else{
      LOGGER.info("Doesn't receive PDF file !");
      result.setErrMsg("Server doesn't receive PDF file !");
    }
    return result;
  }
  
  @RequestMapping(value="/preview/{relPath}/{pdfName}", method=RequestMethod.GET)
  public @ResponseBody PreviewResult preview(@PathVariable String relPath, 
                                             @PathVariable String pdfName){
    long startTs = System.currentTimeMillis();
    
    PreviewResult result = new PreviewResult();
    File source = new File(String.format("%s%s%s%s%s%s", baseSourceFolder, File.separator, relPath, File.separator, pdfName, PDF_FORMAT));
    
    if(source.exists()){
      PDDocument document = null;
      try{
        Date now = new Date();
        
        DateFormat df = new SimpleDateFormat(DF);
        String md5Hex = DigestUtils.md5Hex("" + now.getTime());
        
        String folderName = String.format("%s-%s", df.format(now), md5Hex);
        
        File tempFile = new File(String.format("%s%s%sori%s", baseResultFolder, folderName, File.separator, PDF_FORMAT));
        LOGGER.info("Relocating PDF file to : {}", tempFile.getAbsolutePath());
        tempFile.getParentFile().mkdirs();
        FileUtil.fastChannelCopy(new FileInputStream(source), new FileOutputStream(tempFile));
        
        document = PDDocument.loadNonSeq(tempFile, null);
        
        String[] previews = previewService.getPreview(document, folderName);
        
        result.setSuccess(1);
        result.setPdfName(folderName);
        result.setImagesUrl(previews);
        LOGGER.info("Processed previews in {} milliseconds", System.currentTimeMillis()-startTs);
      }catch(Exception e){
        LOGGER.error("Error while previewing document [{}] : {}", source.getName(), e.getMessage());
        result.setErrMsg(String.format("Error while previewing document [%s] : %s", source.getName(), e.getMessage()));
      }finally{
        if(document != null) try{ document.close(); }catch(IOException e){}
      }
    }else{
      LOGGER.info("Doesn't receive PDF file ! : {}", source.getAbsolutePath());
      result.setErrMsg("Server doesn't receive PDF file !");
    }
    return result;
  }
  
  @SuppressWarnings("unchecked")
  @RequestMapping(value="exc/{relPath}/{pdfName}", method=RequestMethod.GET)
  public @ResponseBody ResponseResult split(@PathVariable String relPath, 
                                            @PathVariable String pdfName,
                                            @RequestParam("resultName") String resultName,
                                            @RequestParam("pages[]") Integer[] ids){
    long startTs = System.currentTimeMillis();
    
    ResponseResult result = new ResponseResult();
    
    if(ids == null || ids.length == 0){
      result.setErrMsg("There is no selected pages !");
      return result;
    }
    Arrays.sort(ids);
    
    File source = new File(String.format("%s%s%sori%s", baseResultFolder, pdfName, File.separator, PDF_FORMAT));
    
    if(source.exists()){
      PDDocument document = null;
      FileOutputStream output = null;
      COSWriter writer = null;
      try{
        document = PDDocument.loadNonSeq(source, null);
        
        List<PDPage> pages = document.getDocumentCatalog().getAllPages();
        int maxPage = pages.size();
        
        int firstReqId = ids[0];
        if(firstReqId < 0) throw new IllegalArgumentException(String.format("No Page number %d", firstReqId));
        
        int lastReqId = ids[ids.length-1];
        if(lastReqId > (maxPage-1)) throw new IllegalArgumentException(String.format("No Page number %d", lastReqId));
        
        PDDocument newDocument = new PDDocument();
        newDocument.setDocumentInformation(document.getDocumentInformation());
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDDocumentCatalog newCatalog = newDocument.getDocumentCatalog();
        newCatalog.setViewerPreferences(catalog.getViewerPreferences());
        // copy global resources to the new pdf document
        newCatalog.getPages().setResources(catalog.getPages().getResources());
        
        for(int i=0; i<ids.length; i++){
          PDPage page = pages.get(ids[i]);
          PDPage imported = newDocument.importPage(page);
          imported.setCropBox(page.findCropBox());
          imported.setMediaBox(page.findMediaBox());
          // only the resources of the page will be copied
          imported.setResources(page.getResources());
          imported.setRotation(page.findRotation());
          // remove page links to avoid copying not needed resources
          List<PDAnnotation> annotations = imported.getAnnotations();
          for(PDAnnotation annotation : annotations){
            if(annotation instanceof PDAnnotationLink){
              PDAnnotationLink link = (PDAnnotationLink)annotation;   
              PDDestination destination = link.getDestination();
              if(destination == null && link.getAction() != null){
                PDAction action = link.getAction();
                if(action instanceof PDActionGoTo){
                  destination = ((PDActionGoTo)action).getDestination();
                }
              }
              if(destination instanceof PDPageDestination){
                // TODO preserve links to pages within the splitted result  
                ((PDPageDestination) destination).setPage(null);
              }
            }
            // TODO preserve links to pages within the splitted result  
            annotation.setPage(null);
          }
        }
        
        String resultFilePath = String.format("%s%s%s%s%s", baseSourceFolder, relPath, File.separator, resultName, PDF_FORMAT);
        
        output = new FileOutputStream(resultFilePath);
        writer = new COSWriter( output );
        writer.write(newDocument);
        
        result.setSuccess(1);
        
        // DELETE temp file
        File srcFldr = source.getParentFile();
        File[] tempFiles = srcFldr.listFiles();
        boolean isDelSuccess = true;
        for(File tmp: tempFiles) isDelSuccess &= tmp.delete();
        if(isDelSuccess) srcFldr.delete();
        
        LOGGER.info("Processed splitting in {} milliseconds", System.currentTimeMillis()-startTs);
      }catch(Exception e){
        LOGGER.error("Error while splitting document [{}] : {}", source.getAbsolutePath(), e.getMessage());
        result.setErrMsg(String.format("Error while splitting document [%s] : %s", pdfName, e.getMessage()));
      }finally{
        if(document != null) try{ document.close(); }catch(IOException e){}
        if(output != null) try{ output.close(); }catch(IOException e){}
        if(writer != null) try{ writer.close(); }catch(IOException e){}
      }
    }else{
      LOGGER.info("Doesn't find PDF file ! : {}", source.getAbsolutePath());
      result.setErrMsg(String.format("Server couldn't find PDF file : %s !", pdfName));
    }
    
    return result;
  }
}
