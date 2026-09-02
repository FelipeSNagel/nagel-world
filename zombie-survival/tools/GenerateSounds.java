import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public final class GenerateSounds {
    private static final float SAMPLE_RATE = 44100.0f;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Uso: GenerateSounds <pasta>");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        write(output.resolve("infected_ambient_1.wav"), synth(2.15, 1181L, 0, false));
        write(output.resolve("infected_ambient_2.wav"), synth(2.55, 2387L, 1, false));
        write(output.resolve("infected_ambient_3.wav"), synth(1.85, 3911L, 2, false));
        write(output.resolve("berserker_roar.wav"), synth(3.45, 99173L, 0, true));
        write(output.resolve("horde_warning.wav"), hordeWarning(7.2, 773341L));
        write(output.resolve("horde_tension.wav"), hordeTension(24.0, 884921L));
    }

    private static byte[] synth(double duration, long seed, int variant, boolean berserker) {
        int samples = (int) (duration * SAMPLE_RATE);
        byte[] pcm = new byte[samples * 2];
        Random random = new Random(seed);
        double phase = 0.0;
        double subPhase = 0.0;
        double filteredNoise = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double attack = Math.min(1.0, t / (berserker ? 0.32 : 0.12));
            double release = Math.min(1.0, (duration - t) / (berserker ? 0.72 : 0.45));
            double envelope = Math.max(0.0, attack * release);
            double progress = t / duration;
            double base = berserker
                ? 68.0 - 29.0 * progress + 5.0 * Math.sin(t * 5.1)
                : 92.0 - variant * 11.0 - 28.0 * progress + 4.0 * Math.sin(t * (3.4 + variant));
            phase += Math.PI * 2.0 * base / SAMPLE_RATE;
            subPhase += Math.PI * 2.0 * (berserker ? 31.0 : 43.0) / SAMPLE_RATE;
            filteredNoise = filteredNoise * 0.965 + (random.nextDouble() * 2.0 - 1.0) * 0.035;
            double voice = Math.sin(phase)
                + 0.52 * Math.sin(phase * 2.01 + 0.4)
                + 0.27 * Math.sin(phase * 3.03 + 1.1)
                + 0.16 * Math.sin(phase * 4.97);
            double throat = 0.72 + 0.28 * Math.sin(t * (berserker ? 10.0 : 7.0 + variant));
            double breath = filteredNoise * (berserker ? 1.8 : 1.15);
            double pulse = berserker
                ? 0.78 + 0.22 * Math.sin(t * 2.7)
                : 0.75 + 0.25 * Math.sin(t * (2.1 + variant * 0.35));
            double sample = (voice * throat + breath + Math.sin(subPhase) * (berserker ? 0.62 : 0.20))
                * envelope * pulse * (berserker ? 0.72 : 0.52);
            if (berserker && t < 0.42) sample += filteredNoise * (1.0 - t / 0.42) * 0.45;
            sample = Math.tanh(sample * (berserker ? 1.9 : 1.45)) * 0.86;
            short value = (short) Math.round(Math.max(-1.0, Math.min(1.0, sample)) * 32767.0);
            pcm[i * 2] = (byte) (value & 0xff);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        return pcm;
    }

    private static byte[] hordeWarning(double duration, long seed) {
        int samples = (int) (duration * SAMPLE_RATE);
        byte[] pcm = new byte[samples * 2];
        Random random = new Random(seed);
        double phase = 0.0;
        double detunedPhase = 0.0;
        double subPhase = 0.0;
        double filteredNoise = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double attack = Math.min(1.0, t / 1.15);
            double release = Math.min(1.0, (duration - t) / 1.25);
            double envelope = Math.max(0.0, attack * release);
            double sweep = 0.5 + 0.5 * Math.sin(Math.PI * 2.0 * 0.19 * t - 1.2);
            double frequency = 112.0 + 72.0 * sweep;
            phase += Math.PI * 2.0 * frequency / SAMPLE_RATE;
            detunedPhase += Math.PI * 2.0 * (frequency * 1.013 + 5.0) / SAMPLE_RATE;
            subPhase += Math.PI * 2.0 * 38.0 / SAMPLE_RATE;
            filteredNoise = filteredNoise * 0.992 + (random.nextDouble() * 2.0 - 1.0) * 0.008;

            double horn = Math.sin(phase)
                + 0.48 * Math.sin(phase * 2.0)
                + 0.22 * Math.sin(phase * 3.0)
                + 0.38 * Math.sin(detunedPhase);
            double distantPulse = 0.72 + 0.28 * Math.sin(Math.PI * 2.0 * 0.43 * t);
            double sample = horn * envelope * distantPulse * 0.43
                + filteredNoise * envelope * 1.4
                + Math.sin(subPhase) * envelope * 0.15;

            double impactTime = t - 5.45;
            if (impactTime >= 0.0) {
                double impactEnvelope = Math.exp(-impactTime * 2.4);
                double impact = Math.sin(Math.PI * 2.0 * (48.0 - impactTime * 7.0) * impactTime);
                sample += impact * impactEnvelope * 0.78 + filteredNoise * impactEnvelope * 0.42;
            }
            sample = Math.tanh(sample * 1.35) * 0.84;
            short value = (short) Math.round(Math.max(-1.0, Math.min(1.0, sample)) * 32767.0);
            pcm[i * 2] = (byte) (value & 0xff);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        return pcm;
    }

    private static byte[] hordeTension(double duration, long seed) {
        int samples = (int) (duration * SAMPLE_RATE);
        byte[] pcm = new byte[samples * 2];
        Random random = new Random(seed);
        double dronePhase = 0.0;
        double dissonantPhase = 0.0;
        double highPhase = 0.0;
        double filteredNoise = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double attack = Math.min(1.0, t / 1.8);
            double release = Math.min(1.0, (duration - t) / 1.8);
            double envelope = Math.max(0.0, attack * release);
            double movement = 0.5 + 0.5 * Math.sin(Math.PI * 2.0 * t / 12.0 - 1.1);
            dronePhase += Math.PI * 2.0 * (43.0 + movement * 3.0) / SAMPLE_RATE;
            dissonantPhase += Math.PI * 2.0 * (91.0 + movement * 7.0) / SAMPLE_RATE;
            highPhase += Math.PI * 2.0 * (127.0 - movement * 5.0) / SAMPLE_RATE;
            filteredNoise = filteredNoise * 0.996
                + (random.nextDouble() * 2.0 - 1.0) * 0.004;

            double heartbeatPosition = t % 2.4;
            double firstBeat = Math.exp(-heartbeatPosition * 8.0);
            double secondBeatTime = heartbeatPosition - 0.34;
            double secondBeat = secondBeatTime >= 0.0 ? Math.exp(-secondBeatTime * 10.0) * 0.72 : 0.0;
            double heartbeat = (firstBeat + secondBeat)
                * Math.sin(Math.PI * 2.0 * (52.0 - heartbeatPosition * 7.0) * heartbeatPosition);

            double drone = Math.sin(dronePhase)
                + 0.36 * Math.sin(dronePhase * 2.01 + 0.7)
                + 0.21 * Math.sin(dissonantPhase)
                + 0.13 * Math.sin(highPhase);
            double distantWind = filteredNoise * (0.65 + movement * 0.45);
            double sample = (drone * 0.34 + heartbeat * 0.54 + distantWind * 1.7) * envelope;
            sample = Math.tanh(sample * 1.22) * 0.72;
            short value = (short) Math.round(Math.max(-1.0, Math.min(1.0, sample)) * 32767.0);
            pcm[i * 2] = (byte) (value & 0xff);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        return pcm;
    }

    private static void write(Path file, byte[] pcm) throws IOException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(pcm);
             AudioInputStream stream = new AudioInputStream(bytes, format, pcm.length / 2L)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file.toFile());
        }
    }
}
