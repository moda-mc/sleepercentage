package cx.mia.moda.sleepercentage;

import moda.plugin.moda.module.IMessage;

public enum SleepercentageMessage implements IMessage {

    SLEEPING("sleeping", "&d{DISPLAYNAME}&7 would like to sleep. &8[&d{CURRENT_SLEEPERS}&8/&5{NEEDED_SLEEPERS}&8]"),
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