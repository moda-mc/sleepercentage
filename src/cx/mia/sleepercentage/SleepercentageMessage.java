package cx.mia.sleepercentage;

import moda.plugin.moda.module.IMessage;

public enum SleepercentageMessage implements IMessage {

    SLEEPING("sleeping", "&b{DISPLAYNAME}&7 would like to sleep. &8[&b{CURRENT_SLEEPERS}&7/&b{NEEDED_SLEEPERS}&8]"),
    SKIP_NIGHT("skip.night", "&7Enough players went to sleep and the night was skipped."),
    SKIP_STORM("skip.storm", "&7Enough players went to sleep and the storm was skipped."),

    ;

    private final String path;
    private final String defaultMessage;

    SleepercentageMessage(final String path, final String defaultMessage) {
        this.path = path;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getPath() { return this.path; }

    @Override
    public String getDefault() { return this.defaultMessage; }

}
