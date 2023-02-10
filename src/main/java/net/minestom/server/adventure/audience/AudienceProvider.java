package net.minestom.server.adventure.audience;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * A generic provider of {@link Audience audiences} or some subtype.
 *
 * @param <A> the type that is provided
 */
public interface AudienceProvider<A> {

    /**
     * Gets all audience members. This returns {@link #players()} combined with
     * {@link #customs()} and {@link #console()}. This can be a costly operation, so it
     * is often preferable to use {@link #server()} instead.
     *
     * @return all audience members
     */
    A all();

    /**
     * Gets all audience members that are of type {@link Player}.
     *
     * @return all players
     */
    A players();

    /**
     * Gets all audience members that are of type {@link Player} and match the predicate.
     *
     * @param filter the predicate
     * @return all players matching the predicate
     */
    A players(Predicate<Player> filter);

    /**
     * Gets the console as an audience.
     *
     * @return the console
     */
    A console();

    /**
     * Gets the combination of {@link #players()} and {@link #console()}.
     *
     * @return the audience of all players and the console
     */
    A server();

    /**
     * Gets all custom audience members stored using the given keyed object.
     *
     * @param keyed the keyed object
     * @return all custom audience members stored using the key of the object
     */
    default A custom(Keyed keyed) {
        return this.custom(keyed.key());
    }

    /**
     * Gets all custom audience members stored using the given key.
     *
     * @param key the key
     * @return all custom audience members stored using the key
     */
    A custom(Key key);

    /**
     * Gets all custom audience members stored using the given keyed object that match
     * the given predicate.
     *
     * @param keyed  the keyed object
     * @param filter the predicate
     * @return all custom audience members stored using the key
     */
    default A custom(Keyed keyed, Predicate<Audience> filter) {
        return this.custom(keyed.key(), filter);
    }

    /**
     * Gets all custom audience members stored using the given key that match the
     * given predicate.
     *
     * @param key    the key
     * @param filter the predicate
     * @return all custom audience members stored using the key
     */
    A custom(Key key, Predicate<Audience> filter);

    /**
     * Gets all custom audience members.
     *
     * @return all custom audience members
     */
    A customs();

    /**
     * Gets all custom audience members matching the given predicate.
     *
     * @param filter the predicate
     * @return all matching custom audience members
     */
    A customs(Predicate<Audience> filter);

    /**
     * Gets all audience members that match the given predicate.
     *
     * @param filter the predicate
     * @return all matching audience members
     */
    A all(Predicate<Audience> filter);

    /**
     * Gets the audience registry used to register custom audiences.
     *
     * @return the registry
     */
    AudienceRegistry registry();
}
