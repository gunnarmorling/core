package org.jboss.webbeans.test;

import static org.jboss.webbeans.test.util.Util.createSimpleWebBean;

import javax.webbeans.manager.Bean;

import org.jboss.webbeans.SimpleBeanImpl;
import org.jboss.webbeans.test.beans.FishPond;
import org.jboss.webbeans.test.beans.Goldfish;
import org.jboss.webbeans.test.beans.RedSnapper;
import org.jboss.webbeans.test.beans.Tuna;
import org.jboss.webbeans.test.beans.TunaFarm;
import org.testng.annotations.Test;

@SpecVersion("PDR")
public class SimpleBeanLifecycleTest extends AbstractTest
{
	
	@Test(groups="beanConstruction") @SpecAssertion(section="3.1.3")
	public void testInjectionOfParametersIntoBeanConstructor()
	{
	   Bean<FishPond> goldfishPondBean = createSimpleWebBean(FishPond.class, manager);
	   Bean<Goldfish> goldfishBean = createSimpleWebBean(Goldfish.class, manager);
	   manager.addBean(goldfishBean);
	   manager.addBean(goldfishPondBean);
	   FishPond fishPond = goldfishPondBean.create();
	   assert fishPond.goldfish != null;
	}
	
	@Test(groups="specialization") @SpecAssertion(section="3.1.4")
   public void testSpecializedBeanAlwaysUsed()
   {
      // TODO Placeholder
      assert false;
   }
	
   @Test(groups="beanLifecycle") @SpecAssertion(section="5.3")
   public void testCreateReturnsInstanceOfBean()
   {
      Bean<RedSnapper> bean = createSimpleWebBean(RedSnapper.class, manager);
      assert bean.create() instanceof RedSnapper;
   }
   
   @Test(groups={"beanLifecycle", "interceptors"}) @SpecAssertion(section="5.3")
   public void testCreateBindsInterceptorStack()
   {
      assert false;
   }
   
   @Test(groups={"beanLifecycle", "decorators"}) @SpecAssertion(section="5.3")
   public void testCreateBindsDecoratorStack()
   {
      assert false;
   }
   
   @Test(groups={"beanLifecycle", "ejb3"}) @SpecAssertion(section="5.3")
   public void testCreateInjectsEjb()
   {
      assert false;
   }
   
   @Test(groups={"beanLifecycle", "ejb3"}) @SpecAssertion(section="5.3")
   public void testCreateInjectsPersistenceContext()
   {
      assert false;
   }
   
   @Test(groups={"beanLifecycle", "commonAnnotations"}) @SpecAssertion(section="5.3")
   public void testCreateInjectsResource()
   {
      assert false;
   }
   
   @Test(groups="injection") @SpecAssertion(section="5.3")
   public void testCreateInjectsFieldsDeclaredInJava()
   {
      SimpleBeanImpl<TunaFarm> tunaFarmBean = createSimpleWebBean(TunaFarm.class, manager);
      Bean<Tuna> tunaBean = createSimpleWebBean(Tuna.class, manager);
      manager.addBean(tunaBean);
      TunaFarm tunaFarm = tunaFarmBean.create();
      assert tunaFarm.tuna != null;
   }
   
   @Test(groups={"beanConstruction", "commonAnnotations"}) @SpecAssertion(section="5.3")
   public void testPostConstructMethodCalled()
   {
      assert false;
   }
   
   @Test(groups="injection") 
   public void testFieldMissingBindingAnnotationsAreNotInjected()
   {
      SimpleBeanImpl<TunaFarm> tunaFarmBean = createSimpleWebBean(TunaFarm.class, manager);
      Bean<Tuna> tunaBean = createSimpleWebBean(Tuna.class, manager);
      manager.addBean(tunaBean);
      TunaFarm tunaFarm = tunaFarmBean.create();
      assert tunaFarm.notInjectedTuna != manager.getInstance(tunaBean);
   }
	
}
