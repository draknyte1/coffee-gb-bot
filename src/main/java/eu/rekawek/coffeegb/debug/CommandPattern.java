package eu.rekawek.coffeegb.debug;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static com.google.common.collect.Maps.newLinkedHashMap;

public class CommandPattern {

    private final List<String> commandNames;

    private final List<CommandArgument> arguments;

    private final Optional<String> description;

    private CommandPattern(Builder builder) {
        this.commandNames = builder.commandNames;
        this.arguments = builder.arguments;
        this.description = Optional.ofNullable(builder.description);
    }

    public boolean matches(String commandLine) {
        return commandNames
                .stream()
                .filter(commandLine::startsWith)
                .map(String::length)
                .map(commandLine::substring)
                .anyMatch(s -> s.isEmpty() || s.charAt(0) == ' ');
    }

    public List<String> getCommandNames() {
        return commandNames;
    }

    public List<CommandArgument> getArguments() {
        return arguments;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public ParsedCommandLine parse(String commandLine) {
        String commandName = commandNames
                .stream()
                .filter(commandLine::startsWith)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Command line [" + commandLine + "] doesn't match command [" + commandNames + "]"));
        List<String> split = split(commandLine.substring(commandName.length()));
        Map<String, String> map = newLinkedHashMap();
        List<String> remaining = Collections.emptyList();
        int i;
        for (i = 0; i < split.size() && i < arguments.size(); i++) {
            String value = split.get(i);
            CommandArgument argDef = arguments.get(i);
            Optional<Set<String>> allowed = argDef.getAllowedValues();
            if (allowed.isPresent()) {
                if (!allowed.get().contains(value)) {
                    throw new IllegalArgumentException("Value " + value + " is not allowed for argument " + argDef.getName() + ". Allowed values: " + allowed.get());
                }
            }
            map.put(argDef.getName(), value);
        }
        if (i < arguments.size()) {
            CommandArgument argDef = arguments.get(i);
            if (argDef.isRequired()) {
                throw new IllegalArgumentException("Argument " + argDef.getName() + " is required");
            }
        }
        if (i < split.size()) {
            remaining = split.subList(i, split.size());
        }
        return new ParsedCommandLine(map, remaining);
    }

    private static List<String> split(String str) {
        List<String> split = new ArrayList<>();
        boolean isEscaped = false;
        StringBuilder currentArg = new StringBuilder();
        for (int i = 0; i <= str.length(); i++) {
            char c;
            if (i < str.length()) {
                c = str.charAt(i);
            } else {
                c = 0;
            }
            if (isEscaped) {
                switch (c) {
                    case '"':
                        isEscaped = false;
                        split.add(currentArg.toString());
                        currentArg.setLength(0);
                        break;

                    case 0:
                        throw new IllegalArgumentException("Missing closing quote");

                    default:
                        currentArg.append(c);
                        break;
                }
            } else {
                switch (c) {
                    case '"':
                        isEscaped = false;
                        break;

                    case ' ':
                    case 0:
                        if (currentArg.length() > 0) {
                            split.add(currentArg.toString());
                            currentArg.setLength(0);
                        }
                        break;

                    default:
                        currentArg.append(c);
                        break;
                }
            }
        }
        return split;
    }

    @Override
    public String toString() {
        return String.format("CommandPattern[%s]", commandNames.toString());
    }

    public static class ParsedCommandLine {

        private Map<String, String> argumentMap;

        private List<String> remainingArguments;

        private ParsedCommandLine(Map<String, String> argumentMap, List<String> remainingArguments) {
            this.argumentMap = argumentMap;
            this.remainingArguments = remainingArguments;
        }

        public String getArgument(String name) {
            return argumentMap.get(name);
        }

        public List<String> getRemainingArguments() {
            return remainingArguments;
        }
    }

    public static class Builder {

        private final List<String> commandNames;

        private final List<CommandArgument> arguments;

        private String description;

        private Builder(String[] commandNames) {
            this.commandNames = getList(commandNames);
            this.arguments = new ArrayList<>();
        }

        public static Builder create(String longName) {
            return new Builder(new String[] {longName});
        }

        public static Builder create(String longName, String shortName) {
            return new Builder(new String[] {longName, shortName});
        }

        public Builder withOptionalArgument(String name) {
            assertNoOptionalLastArgument();
            arguments.add(new CommandArgument(name, false));
            return this;
        }

        public Builder withRequiredArgument(String name) {
            assertNoOptionalLastArgument();
            arguments.add(new CommandArgument(name, true));
            return this;
        }

        public Builder withOptionalValue(String name, String... values) {
            assertNoOptionalLastArgument();
            arguments.add(new CommandArgument(name, false, getSet(values)));
            return this;
        }

        public Builder withRequiredValue(String name, String... values) {
            assertNoOptionalLastArgument();
            arguments.add(new CommandArgument(name, true, getSet(values)));
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        private void assertNoOptionalLastArgument() {
            if (!arguments.isEmpty() && !arguments.get(arguments.size() - 1).isRequired()) {
                throw new UnsupportedOperationException("Can't add argument after the optional one");
            }
        }
        
        private List<String> getList(String[] aParam1){
        	ArrayList<String> aList = new ArrayList<String>();
        	if (aParam1.length > 0) {
        		for (String s : aParam1) {
        			aList.add(s);
        		}
        	}
        	return ImmutableList.copyOf(aList);        	
        }
        
        private Set<String> getSet(String[] aParam1){
        	Set<String> aList = new HashSet<String>();
        	if (aParam1.length > 0) {
        		for (String s : aParam1) {
        			aList.add(s);
        		}
        	}
        	return ImmutableSet.copyOf(aList);        	
        }

        public CommandPattern build() {
            return new CommandPattern(this);
        }
    }

}
