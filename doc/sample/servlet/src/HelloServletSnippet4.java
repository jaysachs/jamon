        if (numString != null) {
            try {
                helloTemplate.setNum(Integer.parseInt(numString));
            }
            catch (NumberFormatException e) {
                helloTemplate.setError(true);
            }
        }
        helloTemplate.render();