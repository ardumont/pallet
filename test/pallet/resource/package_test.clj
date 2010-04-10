(ns pallet.resource.package-test
  (:use [pallet.resource.package] :reload-all)
  (:use [pallet.stevedore :only [script]]
        [pallet.utils :only [sh-script]]
        [clojure.contrib.shell-out :only [sh]]
        [clojure.contrib.duck-streams :only [copy]]
        clojure.test
        pallet.test-utils))

(deftest update-package-list-test
  (is (= "aptitude update "
         (script (update-package-list)))))

(deftest install-package-test
  (is (= "aptitude install -y  java"
         (script (install-package "java")))))


(deftest test-install-example
  (is (= "debconf-set-selections <<EOF
debconf debconf/frontend select noninteractive
debconf debconf/frontend seen false
EOF
aptitude install -y  java\naptitude install -y  rubygems\n"
         (pallet.resource/build-resources []
          (package "java" :action :install)
          (package "rubygems" :action :install)))))

(deftest package-manager-non-interactive-test
  (is (= "debconf-set-selections <<EOF
debconf debconf/frontend select noninteractive
debconf debconf/frontend seen false
EOF
"
         (script (package-manager-non-interactive)))))

(deftest add-scope-test
  (is (= "tmpfile=$(mktemp addscopeXXXX)\ncp -p /etc/apt/sources.list ${tmpfile}\nawk '{if ($1 ~ /^deb/ && ! /multiverse/  ) print $0 \" \" \" multiverse \" ; else print; }'  /etc/apt/sources.list  >  ${tmpfile}  && mv -f ${tmpfile} /etc/apt/sources.list\n"
         (add-scope "deb" "multiverse" "/etc/apt/sources.list")))

  (testing "with sources.list"
    (let [tmp (java.io.File/createTempFile "package_test" "test")]
      (copy "deb http://archive.ubuntu.com/ubuntu/ karmic main restricted
deb-src http://archive.ubuntu.com/ubuntu/ karmic main restricted"
            tmp)
      (is (= {:exit 0, :out "", :err ""}
             (sh-script (add-scope "deb" "multiverse" (.getPath tmp)))))
      (is (= "deb http://archive.ubuntu.com/ubuntu/ karmic main restricted  multiverse \ndeb-src http://archive.ubuntu.com/ubuntu/ karmic main restricted  multiverse \n"
             (slurp (.getPath tmp))))
      (.delete tmp))))

(deftest package-manager*-test
  (is (= "tmpfile=$(mktemp addscopeXXXX)\ncp -p /etc/apt/sources.list ${tmpfile}\nawk '{if ($1 ~ /^deb.*/ && ! /multiverse/  ) print $0 \" \" \" multiverse \" ; else print; }'  /etc/apt/sources.list  >  ${tmpfile}  && mv -f ${tmpfile} /etc/apt/sources.list\n"
         (package-manager* :multiverse)))
  (is (= "aptitude update "
         (package-manager* :update))))

(deftest test-add-multiverse-example
  (is (= "tmpfile=$(mktemp addscopeXXXX)\ncp -p /etc/apt/sources.list ${tmpfile}\nawk '{if ($1 ~ /^deb.*/ && ! /multiverse/  ) print $0 \" \" \" multiverse \" ; else print; }'  /etc/apt/sources.list  >  ${tmpfile}  && mv -f ${tmpfile} /etc/apt/sources.list\naptitude update\n"
         (pallet.resource/build-resources []
          (package-manager :multiverse)
          (package-manager :update)))))