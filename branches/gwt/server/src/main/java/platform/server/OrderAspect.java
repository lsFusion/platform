package platform.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;


@Aspect
@DeclarePrecedence("platform.server.data.translator.TranslateAspect, platform.server.caches.CacheAspect,*")
public class OrderAspect {
}
