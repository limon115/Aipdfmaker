use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::jstring;
use std::path::PathBuf;
use tectonic::driver::ProcessingSessionBuilder;

#[no_mangle]
pub extern "system" fn Java_com_example_domain_services_pdf_TectonicBridge_compileToPdf(
    mut env: JNIEnv,
    _class: JClass,
    context: JObject,
    tex_source: JString,
    bundle_path: JString,
    output_dir: JString,
) -> jstring {
    // 1. Extract Java strings safely outside the panic shield
    let tex_source_str: String = env.get_string(&tex_source).map(|s| s.into()).unwrap_or_default();
    let output_dir_str: String = env.get_string(&output_dir).map(|s| s.into()).unwrap_or_default();
    
    let vm = env.get_java_vm().unwrap();
    let vm_ptr = vm.get_java_vm_pointer() as *mut std::ffi::c_void;
    let context_ptr = context.as_raw() as *mut std::ffi::c_void;

    // 🛡️ 2. THE ULTIMATE SHIELD: Wrap EVERYTHING in catch_unwind
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        
        // Initialize Android Context for app_dirs2
        unsafe {
            ndk_context::initialize_android_context(vm_ptr, context_ptr);
        }

        // Lock ALL Tectonic Linux paths exclusively to the Android cache directory
        std::env::set_var("TECTONIC_CACHE_DIR", &output_dir_str);
        std::env::set_var("XDG_CACHE_HOME", &output_dir_str);
        std::env::set_var("XDG_CONFIG_HOME", &output_dir_str);
        std::env::set_var("XDG_DATA_HOME", &output_dir_str);
        std::env::set_var("SSL_CERT_DIR", "/system/etc/security/cacerts");

        let fc_dir = PathBuf::from(&output_dir_str).join("fontconfig");
        std::fs::create_dir_all(&fc_dir).unwrap_or_default();
        std::env::set_var("FONTCONFIG_FILE", fc_dir.join("fonts.conf").to_string_lossy().to_string());
        std::env::set_var("FONTCONFIG_PATH", fc_dir.to_string_lossy().to_string());

        let mut status = tectonic::status::NoopStatusBackend::default();
        let mut builder = ProcessingSessionBuilder::default();
        builder
            .primary_input_buffer(tex_source_str.as_bytes())
            .tex_input_name("main.tex")
            .build_date(std::time::SystemTime::now())
            .output_dir(PathBuf::from(&output_dir_str))
            .output_format(tectonic::driver::OutputFormat::Pdf);

        // 🔴 We missed this before! builder.create() is now safely trapped inside the shield!
        let mut session = match builder.create(&mut status) {
            Ok(s) => s,
            Err(e) => return format!("Error: Failed to create session - {}", e),
        };

        match session.run(&mut status) {
            Ok(_) => {
                let pdf_path = PathBuf::from(&output_dir_str).join("main.pdf");
                pdf_path.to_string_lossy().into_owned()
            }
            Err(e) => format!("Error: Compilation failed - {}", e),
        }
    }));

    // 3. Process the result and safely return it to Kotlin
    let output_path = match result {
        Ok(path) => path,
        Err(panic_err) => {
            let msg = if let Some(s) = panic_err.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_err.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown Native Rust Panic".to_string()
            };
            format!("Error: Native Rust Panic Caught! - {}", msg)
        }
    };

    let output = env.new_string(output_path).unwrap();
    output.into_raw()
}
