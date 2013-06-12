package io.cloudsoft.opengamma.demo;

import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.ssh.CommonCommands;

public class OpenGammaDemoSshDriver extends AbstractSoftwareProcessSshDriver implements OpenGammaDemoDriver {

    public OpenGammaDemoSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void install() {
        DownloadResolver resolver = ((EntityInternal)entity).getManagementContext().getEntityDownloadsManager().newDownloader(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();
        
        List<String> commands = new LinkedList<String>();
        commands.addAll(CommonCommands.downloadUrlAs(urls, saveAs));
        commands.add(CommonCommands.INSTALL_TAR);
        commands.add("tar xvfz "+saveAs);

        newScript(INSTALLING).
                updateTaskAndFailOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public void customize() {
        DownloadResolver resolver = ((EntityInternal)entity).getManagementContext().getEntityDownloadsManager().newDownloader(this);
        String saveAs = resolver.getUnpackedDirectoryName(resolver.getFilename());
        // Copy the install files to the run-dir
        newScript(CUSTOMIZING)
            .updateTaskAndFailOnNonZeroResultCode()
                .body.append("cp -r "+getInstallDir()+"/"+resolver.getUnpackedDirectoryName("opengamma")+" "+"opengamma")
                .body.append("cd opengamma")
                .body.append("scripts/init-og-examples-db.sh")
                .execute();
        
        // TODO set up OG web port, and whatever else we want to customize
    }

    @Override
    public void launch() {
        String mode = getEntity().getConfig(OpenGammaDemoServer.DEBUG_MODE) ? "debug" : "start";
        newScript(MutableMap.of("usePidFile", true), LAUNCHING).
            updateTaskAndFailOnNonZeroResultCode().
            body.append("cd opengamma", 
                "nohup scripts/og-examples.sh "+mode+" > out.log 2> err.log < /dev/null &").
            execute();
    }


    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", true), CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(MutableMap.of("usePidFile", true), STOPPING).execute();
    }

    @Override
    public void kill() {
        newScript(MutableMap.of("usePidFile", true), KILLING).execute();
    }


}