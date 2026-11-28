# MLZBoatLag

A Paper 1.21.4 plugin for MLZ. When you hit another boat, you **don't get shot back**. You're placed right behind the boat you hit (or right beside it, for a side bump), and **you keep your speed**. Where you end up depends on where the other boat is, not on your ping, so a bad-ping rider gets treated the same as everyone else.

It works on top of normal vanilla boat lag. You just add it (plus ProtocolLib) to the `plugins` folder; nothing else on the server changes. If a server runs NoBoatLag, take that out first.

## How it works (short version)

1. Your game keeps telling the server where your boat is.
2. When the server thinks you went through another boat, it sends a correction: "you're actually back HERE."
3. That arrives one ping round trip late, so in vanilla you get shot back about **speed × ping**. High ping = shot back further.
4. This plugin catches that correction and instead moves you the **smallest** amount that stops your boat overlapping theirs: back (right behind them) or sideways (right beside them), whichever is shorter. You keep your sideways spot, so you can still pull out and go around. If you're not actually overlapping, you don't get moved at all.
5. **Ping balancing:** the server compares the lightning rods, and a high-ping rider's rod trails their real boat further (about speed × half their ping). So before deciding anything, the plugin pushes each rod forward by that rider's own lag to get where everyone *really* is. If the rider being corrected was really ahead, the correction is cancelled and they keep driving. Only the rider who was really behind gets parked behind.
6. That message only carries a position, not a speed, so your game keeps your momentum. If you're faster than them you'll keep bumping them and keep getting held right behind, like being stuck behind someone in a real race.

It only touches corrections that happen right after you were near another boat. Walls, warps and `/tp` work like normal.

## Files

| File | What it does |
|---|---|
| `MLZBoatLag.java` | Starts everything up and connects the pieces |
| `CollisionResolver.java` | **The main rule.** Works out the spot behind/beside the other boat |
| `BoatPacketListener.java` | Catches the boat position packets and applies the rule |
| `ContactTracker.java` | Every tick: each rider and their closest boat, where both are, how fast, and the other rider's ping |
| `PingCompensation.java` | **Ping balancing.** Works out where each rider really is, and who was really ahead |
| `RiderState.java` | Per-rider info (position, driving direction), plus a safety valve so nobody gets stuck |
| `Point.java` | Small x/y/z helper for the math |
| `PacketPositions.java` | Reads and writes x/y/z inside the packet |
| `BoatLagSettings.java` | Loads `config.yml` |
| `BoatLagCommand.java` | `/boatlag reload` and `/boatlag debug` |
| `PingCompensationTest.java` | Tests: a high-ping rider who was really ahead isn't punished |
| `CollisionResolverTest.java` | Tests: rear-end puts you behind, low and high ping end up in the same spot, pulling out to pass isn't snapped back in line |

## Setting up (GitHub Codespaces)

1. Make a new GitHub repo (for example `Swishyyyy/MLZBoatLag`) and upload everything from this zip, including the hidden `.devcontainer` folder.
2. On the repo page, click **Code → Codespaces → Create codespace**.
3. Wait for it to finish setting up. It installs Java 21 and Gradle and creates `./gradlew` for you.
4. In the terminal at the bottom, run:
   ```
   ./gradlew build
   ```
5. The plugin jar is in `build/libs/MLZBoatLag-1.0.0.jar`. Right-click it → **Download**.

If `./gradlew` doesn't exist, run `gradle wrapper` once first.

## Testing

- `./gradlew test` runs the math tests.
- `./gradlew runServer` starts a test server with ProtocolLib already installed. The first time, it stops and asks you to accept the EULA: open `run/eula.txt`, change it to `eula=true`, and run the command again. This is mainly for checking that the plugin loads with no errors.
- **The real test** needs two people in boats on a server with real ping, ideally the MLZ test server:
  1. Put the jar plus ProtocolLib in `plugins/`, then restart.
  2. Both riders run `/boatlag debug`. To see where the server thinks each rider is, use the separate TestServerUtils plugin.
  3. Rear-end each other at full speed. Your action bar shows how far vanilla would have moved you and where you were placed instead.
  4. What should happen: if you were really behind, you stay right behind them without being thrown back or losing speed. If you were really ahead (even with worse ping), the action bar says `you were really ahead, correction cancelled` and you keep going.

## Tuning (`plugins/MLZBoatLag/config.yml`)

- `rear-gap-blocks` / `side-gap-blocks`: how close you're parked to the other boat's server position (its lightning rod). 1.4 is touching; it can't go below 1.375 (a boat's length) or the boats would overlap.
- `other-boat-screen-delay-millis`: 0 (default) parks you right behind the other boat's lightning rod. If riders lose speed right after being parked, raise it a little (50–100).
- `contact-memory-millis`: must be longer than the worst ping round trip in a heat. 1000 is safe.
- `worlds`: limit it to the speedway world if you want.

Run `/boatlag reload` after editing.

## Not yet confirmed

- This hasn't been built against the real Paper/ProtocolLib jars or run on a server yet. Check the first `./gradlew build` and the first server startup for errors.
- Keeping momentum relies on the vanilla client keeping its speed when it gets a position correction. If riders still lose speed after a hit, try the screen-delay setting first. If that doesn't fix it, the plugin may also need to send the rider's speed back to them.
- Use ProtocolLib **5.4.0 or newer**. Older versions don't support 1.21.4.
