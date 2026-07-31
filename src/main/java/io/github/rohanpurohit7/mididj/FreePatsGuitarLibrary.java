package io.github.rohanpurohit7.mididj;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Downloads and installs the small CC0 FreePats clean electric-guitar SF2. */
public final class FreePatsGuitarLibrary {
    public static final URI CLEAN_SMALL = URI.create("https://freepats.zenvoid.org/ElectricGuitar/FSBS-EGuitar/EGuitarFSBS-bridge-clean-small-SF2-20220911.7z");
    public static final URI JAZZ_SMALL = URI.create("https://freepats.zenvoid.org/ElectricGuitar/FSBS-EGuitar/EGuitarFSBS-bridge-jazz-small-SF2-20220911.7z");
    public static final String LICENSE = "CC0 1.0 — FreePats FSBS Electric Guitar";

    private FreePatsGuitarLibrary() {}

    public static Path libraryDir(){ return Path.of(System.getProperty("user.home"),".studio-guitar-improv","soundfonts"); }
    public static Path installedCleanSf2(){
        Path dir=libraryDir();
        if(!Files.isDirectory(dir)) return null;
        try(var files=Files.list(dir)){
            return files.filter(p->p.getFileName().toString().toLowerCase().endsWith(".sf2")).findFirst().orElse(null);
        }catch(Exception ex){ return null; }
    }

    public static Path installDefault() throws Exception { return install(CLEAN_SMALL,"freepats-clean-small.7z"); }
    public static Path installJazz() throws Exception { return install(JAZZ_SMALL,"freepats-jazz-small.7z"); }

    private static Path install(URI uri,String archiveName) throws Exception {
        Files.createDirectories(libraryDir());
        Path archive=libraryDir().resolve(archiveName);
        HttpRequest request=HttpRequest.newBuilder(uri).header("User-Agent","StudioGuitarImprov/1.0").build();
        HttpResponse<InputStream> response=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
                .send(request,HttpResponse.BodyHandlers.ofInputStream());
        if(response.statusCode()/100!=2) throw new IllegalStateException("SoundFont download failed: HTTP "+response.statusCode());
        try(InputStream in=response.body()){ Files.copy(in,archive,StandardCopyOption.REPLACE_EXISTING); }
        Path sf2=null;
        try(SevenZFile sevenZ=SevenZFile.builder().setFile(archive.toFile()).get()){
            SevenZArchiveEntry entry;
            while((entry=sevenZ.getNextEntry())!=null){
                if(entry.isDirectory()||!entry.getName().toLowerCase().endsWith(".sf2")) continue;
                String safe=Path.of(entry.getName()).getFileName().toString();
                sf2=libraryDir().resolve(safe);
                try(var out=Files.newOutputStream(sf2)){ sevenZ.getInputStream(entry).transferTo(out); }
                break;
            }
        }
        Files.deleteIfExists(archive);
        if(sf2==null) throw new IllegalStateException("Downloaded archive contained no SF2 file");
        return sf2;
    }
}
