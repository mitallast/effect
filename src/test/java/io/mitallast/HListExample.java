package io.mitallast;

import io.mitallast.hlist.HList;
import io.mitallast.product.Product3;

public class HListExample {

    static class Case3 extends Product3<Long, Long, String> {
        public Case3(Long id, Long count, String title) {
            super(id, count, title);
        }

        public Long id() {
            return t1();
        }

        public Long count() {
            return t2();
        }

        public String title() {
            return t3();
        }
    }

    public static void main(String... args) {
        var case3 = new Case3(123L, 143L, "324234");
        System.out.println(case3);
        var hlist = case3.toHList();
        var case3copy = HList.apply(hlist, Case3::new);
        System.out.println(hlist);
        System.out.println(case3copy);
    }
}
