package com.shortlink.common.util;
import cn.hutool.core.lang.hash.MurmurHash;
import org.springframework.stereotype.Component;

@Component
public class ShortLinkUtil {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 将长链接转为短码（MurmurHash + Base62）
     */
    public static String generateShortCode(String longUrl) {
        long hash = MurmurHash.hash64(longUrl);
        // 取绝对值，避免负数
        hash = Math.abs(hash);
        return toBase62(hash);
    }

    private static String toBase62(long num) {
        if (num == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            int remainder = (int) (num % 62);
            sb.append(BASE62.charAt(remainder));
            num /= 62;
        }
        return sb.reverse().toString();
    }

    /**
     * 根据短码生成库表分片键（例如取前两位字符的哈希值）
     * 这里简单用短码的hashCode对库表总数取模
     */
    public static int getShardingKey(String shortCode) {
        return Math.abs(shortCode.hashCode()) % (32 * 256);
    }
}