$(function() {

    // User Register Validation
    var $userRegister = $("#userRegister");

    $userRegister.validate({
        rules: {
            name: {
                required: true,
                lettersonly: true
            },
            email: {
                required: true,
                email: true
            },
            mobileNumber: {
                required: true,
                numericOnly: true,
                minlength: 10,
                maxlength: 12
            },
            password: {
                required: true
            },
            confirmpassword: {
                required: true,
                equalTo: '#password'
            },
            address: {
                required: true,
                all: true
            },
            city: {
                required: true
            },
            state: {
                required: true
            },
            pincode: {
                required: true,
                numericOnly: true
            },
            img: {
                required: true
            }
        },
        messages: {
            name: {
                required: 'Name is required',
                lettersonly: 'Invalid name'
            },
            email: {
                required: 'Email is required',
                email: 'Invalid email'
            },
            mobileNumber: {
                required: 'Mobile number is required',
                numericOnly: 'Invalid mobile number',
                minlength: 'Minimum 10 digits required',
                maxlength: 'Maximum 12 digits allowed'
            },
            password: {
                required: 'Password is required'
            },
            confirmpassword: {
                required: 'Confirm password is required',
                equalTo: 'Passwords do not match'
            },
            address: {
                required: 'Address is required',
                all: 'Invalid address'
            },
            city: {
                required: 'City is required'
            },
            state: {
                required: 'State is required'
            },
            pincode: {
                required: 'Pincode is required',
                numericOnly: 'Invalid pincode'
            },
            img: {
                required: 'Profile image is required'
            }
        },
        submitHandler: function(form) {
            form.submit();  // Submit the form if validation passes
        }
    });

    // Orders Validation
    var $orders = $("#orders");

    $orders.validate({
        rules: {
            firstName: {
                required: true,
                lettersonly: true
            },
            lastName: {
                required: true,
                lettersonly: true
            },
            email: {
                required: true,
                email: true
            },
            mobileNo: {
                required: true,
                numericOnly: true,
                minlength: 10,
                maxlength: 12
            },
            address: {
                required: true,
                all: true
            },
            city: {
                required: true
            },
            state: {
                required: true
            },
            pincode: {
                required: true,
                numericOnly: true
            },
            paymentType: {
                required: true
            }
        },
        messages: {
            firstName: {
                required: 'First name is required',
                lettersonly: 'Invalid first name'
            },
            lastName: {
                required: 'Last name is required',
                lettersonly: 'Invalid last name'
            },
            email: {
                required: 'Email is required',
                email: 'Invalid email'
            },
            mobileNo: {
                required: 'Mobile number is required',
                numericOnly: 'Invalid mobile number',
                minlength: 'Minimum 10 digits required',
                maxlength: 'Maximum 12 digits allowed'
            },
            address: {
                required: 'Address is required',
                all: 'Invalid address'
            },
            city: {
                required: 'City is required'
            },
            state: {
                required: 'State is required'
            },
            pincode: {
                required: 'Pincode is required',
                numericOnly: 'Invalid pincode'
            },
            paymentType: {
                required: 'Select a payment type'
            }
        },
        submitHandler: function(form) {
            form.submit();  // Submit the form if validation passes
        }
    });

	var $reviews = $("#reviews");
	$reviews.validate({
	        rules: {
	            imageUrl: {
	                required: true,
	                lettersonly: false
	            },
	   
	        },
	        messages: {
	            imageUrl: {
	                required: 'Image Is Required',
	                lettersonly: 'Invalid first name'
	            },
	        
	        },
	        submitHandler: function(form) {
	            form.submit();  // Submit the form if validation passes
	        }
	    });

    // Reset Password Validation
    var $resetPassword = $("#resetPassword");

    $resetPassword.validate({
        rules: {
            password: {
                required: true
            },
            cpassword: {  // Match the name in the form
                required: true,
                equalTo: '#password'  // Refer to the id of the password input field
            }
        },
        messages: {
            password: {
                required: 'Password is required'
            },
            cpassword: {  // Match the name in the form
                required: 'Confirm password is required',
                equalTo: 'Passwords do not match'
            }
        },
        submitHandler: function(form) {
            form.submit();  // Submit the form if validation passes
        }
    });

    // Custom Validation Methods
    jQuery.validator.addMethod('lettersonly', function(value, element) {
        return /^[a-zA-Z\s-]+$/.test(value);  // Allow letters, spaces, and hyphens
    });

    jQuery.validator.addMethod('all', function(value, element) {
        return /^[a-zA-Z0-9_,.\s-]+$/.test(value);  // Allow alphanumeric and some special characters
    });

    jQuery.validator.addMethod('numericOnly', function(value, element) {
        return /^[0-9]+$/.test(value);  // Allow only digits
    });

});
